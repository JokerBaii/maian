#!/usr/bin/env python3
"""Populate only the local H2 preview with fictional, connected business fixtures.

No production URL is accepted. The UI uses ordinary business labels; provenance,
IDs and an online backup are retained under .cache/preview/. Re-running is a no-op.
"""
import csv
from collections import Counter
from datetime import datetime, timedelta
import fcntl
import json
import math
import os
from pathlib import Path
import random
import subprocess
import tempfile
import uuid
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parent.parent
RUNTIME = ROOT / '.cache' / 'preview'
VERSION = 'neighborhood-v1'
NOW = datetime.now(ZoneInfo('Asia/Shanghai')).replace(microsecond=0)
RNG = random.Random(20261025)


def identifier(key):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f'maian/preview/{VERSION}/{key}'))


def literal(value):
    if value is None:
        return 'NULL'
    if isinstance(value, bool):
        return 'TRUE' if value else 'FALSE'
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, datetime):
        return "TIMESTAMP WITH TIME ZONE '" + value.isoformat() + "'"
    return "'" + str(value).replace("'", "''") + "'"


def normalize_initial(statements, profiles, initial):
    """Rename only known original fixture labels, leaving edited values intact."""
    for phone, previous, name in [('19900000001','演示求救者','林辰'),
                                 ('19900000002','演示志愿者','周予安'),
                                 ('19900000003','演示管理员','陈嘉宁')]:
        statements.append(f'UPDATE user_profiles SET nickname={literal(name)} '
                          f'WHERE id={literal(profiles[phone])} AND nickname={literal(previous)};')
    for previous, name in [('待归还 AED','云栖里便民服务站 AED'),('备用 AED A','青禾路社区驿站 AED'),
                           ('备用 AED B','锦溪街文体中心 AED'),('待审核 AED','晴川创客空间 AED'),
                           ('故障维护 AED','云栖里综合服务点 AED')]:
        statements.append('UPDATE emergency_devices SET '+
            f'name={literal(name)}, address={literal(name.removesuffix(" AED")+"一层服务台旁")}, '+
            "owner='云栖社区设备管理组', instructions='设备位于服务台附近，取用与交接请联系现场工作人员。', "+
            "review_note=CASE WHEN reviewed_at IS NULL THEN NULL ELSE '已核对设备登记资料' END "+
            f'WHERE registered_by_user_id={literal(profiles["19900000001"])} '+
            f'AND name={literal("【演示】"+previous)} AND address=\'【演示】虚构设备位置\';')
    for key in ('completedRescueId','canceledRescueId'):
        rescue = literal(initial[key])
        statements.append('UPDATE rescue_calls SET address=\'云栖里便民服务站门口\' '+
                          f'WHERE id={rescue} AND address=\'【演示】测试场景集合点\';')
        statements.append('UPDATE rescue_calls SET matched_snapshot_address=(SELECT address FROM emergency_devices d '+
                          'WHERE d.id=rescue_calls.matched_device_id) '+
                          f'WHERE id={rescue} AND matched_snapshot_address=\'【演示】虚构设备位置\';')
        statements.append("UPDATE rescue_calls SET description='现场人员身体不适，已联络工作人员协助处理。' "+
                          f"WHERE id={rescue} AND description LIKE '虚构的%';")
        statements.append("UPDATE rescue_events SET summary=REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(summary, "+
            "'【演示】',''),'演示固定','固定'),'演示现场','求助现场'),'演示救援','现场协助'),'演示任务','本次任务') "+
            f'WHERE rescue_call_id={rescue};')
    for previous, title, body in [
        ('AED 取用与归还记录','AED 取用与归还：把交接信息记录完整','设备取用与归还时，核对位置、现场联系人和交接情况。实际完成交接后再在任务中确认归还，遇到设备异常及时联系管理方。'),
        ('设备故障如何上报','发现设备异常，怎样提交清楚的故障说明','发现设备外观、标识或状态异常时，记录所在位置与可见情况，从设备详情提交说明。管理方核实期间暂停开放，恢复使用前由授权人员完成检查。'),
        ('救援结束后查看复盘','救援结束后，核对流程与后续待办','打开救援详情整理已有事件，核对响应、到场与确认时间。检查是否仍有设备归还和评价事项，缺失的记录应核实后补充。'),
        ('待审核的设备管理经验','社区 AED 点位维护经验','定期核对点位入口、楼层指引与开放时段，位置发生变化后及时提交更新说明，便于来访人员查找。')]:
        old = literal('【演示】'+previous)
        statements.append(f'UPDATE science_contents SET title={literal(title)}, body={literal(body)}, '+
                          f'summary={literal(body[:100])}, author=\'云栖志愿服务队\' WHERE title={old} '+
                          f'AND source_submission_id IN (SELECT id FROM science_submissions WHERE user_id={literal(profiles["19900000001"])});')
        statements.append(f'UPDATE science_submissions SET title={literal(title)}, content={literal(body)}, '+
                          "review_note=CASE WHEN reviewed_at IS NULL THEN NULL ELSE '已核对内容范围' END "+
                          f'WHERE title={old} AND user_id={literal(profiles["19900000001"])};')
    statements.append('UPDATE coordination_tasks SET device_name=(SELECT name FROM emergency_devices d '+
        'WHERE d.id=coordination_tasks.device_id) WHERE device_id IN (SELECT id FROM emergency_devices '+
        f'WHERE registered_by_user_id={literal(profiles["19900000001"])} AND owner=\'云栖社区设备管理组\') '+
        "AND device_name LIKE '【演示】%';")


def main():
    os.umask(0o077)
    # Refuse missing or redirected preview directories/files. Never create a database.
    if (RUNTIME.resolve() != RUNTIME or not (RUNTIME / 'demo.json').is_file()
            or not (RUNTIME / 'preview.mv.db').is_file()
            or (RUNTIME / 'preview.mv.db').is_symlink()):
        raise SystemExit('请先启动本仓库的本机预览环境；仅支持 .cache/preview/preview 数据库。')
    with (RUNTIME / 'seed.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        jars = sorted((Path.home() / '.m2/repository/com/h2database/h2').glob('*/h2-*.jar'),
                      key=lambda p: tuple(int(n) for n in p.parent.name.split('.')))
        if not jars:
            raise SystemExit('缺少 H2 依赖，请先运行本机预览。')
        java = [str(RUNTIME / 'tools/jdk/bin/java'), '-Dh2.bindAddress=127.0.0.1', '-cp', str(jars[-1])]
        connection = ['-url', f'jdbc:h2:file:{RUNTIME}/preview;MODE=MySQL;AUTO_SERVER=TRUE;IFEXISTS=TRUE',
                      '-user', 'sa', '-password', '']

        def shell(sql):
            result = subprocess.run(java + ['org.h2.tools.Shell'] + connection + ['-sql', sql],
                                    capture_output=True, text=True, check=True)
            if 'Error:' in result.stdout or 'Exception' in result.stderr:
                raise RuntimeError(result.stdout + result.stderr)

        with tempfile.TemporaryDirectory(prefix='maian-seed-') as temporary:
            temporary = Path(temporary)

            def query(sql):
                target = temporary / 'query.csv'
                shell(f'CALL CSVWRITE({literal(target)}, {literal(sql)})')
                with target.open() as file:
                    return list(csv.DictReader(file))

            if query(f'SELECT id FROM emergency_devices WHERE id={literal(identifier("device/0"))}'):
                print('这批场景数据已经导入，保留现有操作结果，不重复写入。')
                return
            profiles = {row['PHONE']: row['ID'] for row in query(
                "SELECT id, phone FROM user_profiles WHERE phone IN ('19900000001','19900000002','19900000003')")}
            if len(profiles) != 3:
                raise SystemExit('缺少初始预览账号，停止写入。')
            user, volunteer = profiles['19900000001'], profiles['19900000002']
            statements, counts, manifest = [], Counter(), {}
            normalize_initial(statements, profiles, json.loads((RUNTIME / 'demo.json').read_text()))

            def add(table, **row):
                statements.append(f'INSERT INTO {table} ({",".join(row)}) VALUES '
                                  f'({",".join(literal(v) for v in row.values())});')
                counts[table] += 1

            # Fictional facilities in the same Hangzhou coordinate range as the initial preview.
            sites = [
                ('云栖里社区服务中心', '云栖里社区服务中心一层接待台旁', 120.1508, 30.2807, '00:00', '00:00'),
                ('青禾邻里中心', '青禾路邻里中心北门内侧', 120.1534, 30.2822, '08:00', '20:00'),
                ('星桥商务楼', '星桥商务楼 A 座一层大堂', 120.1478, 30.2794, '00:00', '00:00'),
                ('澄心运动公园', '澄心公园东入口游客服务处', 120.1552, 30.2773, '06:00', '22:00'),
                ('知行书院', '知行书院图书馆一楼服务台', 120.1445, 30.2831, '07:00', '21:00'),
                ('悦邻生活广场', '悦邻生活广场一层西侧客服中心', 120.1580, 30.2810, '09:00', '22:00'),
                ('锦和社区党群服务站', '锦和社区党群服务站门厅', 120.1516, 30.2854, '08:30', '18:00'),
                ('清溪地铁接驳站', '清溪接驳站南侧值班室外', 120.1462, 30.2766, '06:00', '23:00'),
                ('望舒科技园', '望舒科技园三号楼一层前台', 120.1592, 30.2852, '00:00', '00:00'),
                ('河畔文化馆', '河畔文化馆入口右侧咨询台', 120.1412, 30.2805, '09:00', '20:30'),
                ('云栖志愿服务车', '云栖里社区服务中心停车区', 120.1521, 30.2798, '00:00', '00:00'),
                ('青禾巡护服务车', '青禾邻里中心北侧停车区', 120.1548, 30.2830, '00:00', '00:00'),
            ]
            for i, (name, address, lng, lat, opens, closes) in enumerate(sites):
                mobile = i >= 10
                status = {6: 'DISABLED', 8: 'PENDING_REVIEW', 9: 'DISABLED'}.get(i, 'AVAILABLE')
                at = NOW - timedelta(days=60-i*3)
                add('emergency_devices', id=identifier(f'device/{i}'), name=f'{name} AED', address=address,
                    longitude=lng, latitude=lat, type='MOBILE' if mobile else 'FIXED', category='AED',
                    status=status, owner='云栖志愿服务队' if mobile else name, owner_phone='19900000002' if mobile else '19900000001',
                    registered_by_user_id=volunteer if mobile else user, created_at=at,
                    expire_date=(NOW.date()+timedelta(days=500+i*31)).isoformat(), version=0,
                    last_location_at=NOW-timedelta(minutes=3 if i == 10 else 90) if mobile else None,
                    vehicle_info='社区志愿服务车' if mobile else None, service_range=3 if mobile else None,
                    instructions='设备位于服务台附近，可联系现场工作人员协助查找。' if status != 'DISABLED'
                        else '巡检发现状态异常，已暂停开放，等待维护人员核实。',
                    reviewed_at=None if status == 'PENDING_REVIEW' else at+timedelta(hours=3),
                    review_note=None if status == 'PENDING_REVIEW' else '已核对设备资料与开放时段')
                for day, position in zip(['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'], range(7)):
                    add('emergency_device_service_windows', device_id=identifier(f'device/{i}'),
                        position=position, day_of_week=day, opens_at=opens, closes_at=closes)
                if status == 'DISABLED':
                    task = identifier(f'coordination/{i}')
                    add('coordination_tasks', id=task, device_id=identifier(f'device/{i}'), device_name=f'{name} AED',
                        owner_user_id=user, event_key=f'{VERSION}/fault/{i}', trigger_type='FAULT_REPORTED',
                        status='PENDING', execution_mode='PENDING', summary='已收到巡检故障上报，等待检查资源与救援关联',
                        created_at=NOW, updated_at=NOW, actions_applied=False, attention_required=False, version=0)
                    add('coordination_steps', task_id=task, position=0, module_name='设备管理',
                        summary='巡检发现设备异常，已停止开放并提交协同检查', occurred_at=NOW)

            # Full terminal histories only: do not create live requests, reservations or presence.
            rescue_ids = []
            for i, days in enumerate([1, 3, 5, 8, 12, 17, 23, 28]):
                rescue = identifier(f'rescue/{i}')
                rescue_ids.append(rescue)
                name, address, lng, lat, *_ = sites[i % 6]
                at = (NOW - timedelta(days=days)).replace(hour=9+i, minute=13+i*3)
                cancelled = i in (3, 6)
                response, arrival = 24+i*7, 155+i*16
                stages = [(0,'CREATED',user,'已提交求助位置与现场信息'),
                          (1,'MATCHING_STARTED',None,'开始检查附近开放的 AED')]
                if cancelled:
                    stages += [(48,'USER_CANCELLED',user,'现场情况已变化，求助者取消本次请求')]
                else:
                    stages += [(3,'AED_ASSIGNED',None,'已匹配附近 AED 并预留设备'),
                               (response,'ACCEPTED',volunteer,'志愿者已响应，前往设备取用点'),
                               (response+42,'ARRIVED_AT_AED',volunteer,'已到达 AED 取用点'),
                               (response+56,'AED_PICKED_UP',volunteer,'已确认取用设备，前往现场'),
                               (arrival,'ARRIVED_AT_REQUESTER',volunteer,'已到达求助者所在位置'),
                               (arrival+12,'RESCUE_STARTED',volunteer,'已开始现场协助并记录进度'),
                               (620+i*33,'COMPLETION_SUBMITTED',volunteer,'现场协助结束，已提交完成确认'),
                               (680+i*33,'COMPLETION_CONFIRMED',user,'求助者已确认流程完成'),
                               (1050+i*33,'AED_RETURNED',volunteer,'设备已归还原位，完成交接核对')]
                row = dict(id=rescue, urgency='CRITICAL', status='USER_CANCELLED' if cancelled else 'COMPLETED',
                    latitude=lat+.0008, longitude=lng+.0006, address=f'{name}附近步行区域',
                    description='现场有人突然身体不适，已联系现场工作人员，需要志愿者协助。' if not cancelled
                        else '现场人员已接手处理，取消本次求助。', requested_by_user_id=user,
                    client_request_id=f'{VERSION}/rescue/{i}', created_at=at,
                    updated_at=at+timedelta(seconds=stages[-1][0]), match_deadline_at=at+timedelta(seconds=601),
                    event_sequence=len(stages), version=0)
                if not cancelled:
                    row.update(responder_user_id=volunteer, matched_device_id=identifier(f'device/{i%6}'),
                        matched_at=at+timedelta(seconds=3), matched_distance_meters=180+i*37,
                        estimated_arrival_seconds=arrival, match_strategy='FIXED_RUNNER',
                        matched_snapshot_latitude=lat, matched_snapshot_longitude=lng, matched_snapshot_address=address,
                        aed_custody_status='RETURNED', accepted_at=at+timedelta(seconds=response),
                        arrived_at_aed_at=at+timedelta(seconds=response+42), arrived_at=at+timedelta(seconds=arrival),
                        rescue_started_at=at+timedelta(seconds=arrival+12),
                        completion_submitted_at=at+timedelta(seconds=620+i*33),
                        confirmation_deadline_at=at+timedelta(seconds=1220+i*33),
                        completed_at=at+timedelta(seconds=680+i*33), aed_returned_at=at+timedelta(seconds=1050+i*33))
                add('rescue_calls', **row)
                if not cancelled and i != 7:
                    add('rescue_feedback', id=identifier(f'feedback/{i}'), rescue_call_id=rescue,
                        from_user_id=user, to_user_id=volunteer, rating=4 if i == 4 else 5,
                        comment=['响应及时，沟通清楚，辛苦了。','设备位置说明很清楚，交接顺利。','全程有进度记录，处理后也说明了后续事项。'][i%3],
                        created_at=at+timedelta(seconds=1400+i*33))
                for sequence, (seconds, kind, actor, summary) in enumerate(stages, 1):
                    add('rescue_events', id=identifier(f'rescue/{i}/event/{sequence}'), rescue_call_id=rescue,
                        sequence_no=sequence, type=kind, actor_user_id=actor, summary=summary,
                        created_at=at+timedelta(seconds=seconds))

            articles = [
                ('device','路过的 AED，怎样记住它的位置','记住入口、楼层和附近地标，比只记一个定位点更方便。','留意设备所在的入口、楼层与服务台位置。查看平台记录的开放时段和取用说明，经过时可以核对标识是否清楚。发现位置或开放时间变化，可通过设备详情反馈，由管理方核实后更新。'),
                ('device','社区 AED 日常巡检，哪些信息值得记录','从位置标识到开放时段，让每次巡检留下可核对的记录。','巡检时记录设备位置标识、柜体外观、开放时段和现场联系方式是否有变化。发现异常时拍下相关位置并提交故障说明，暂停开放的设备由管理方核实恢复。设备内部检查与维护应按厂商要求交由授权人员进行。'),
                ('emergency','发起求助时，怎样把位置说清楚','入口、楼层、地标与现场联系人，帮助协作人员找到你。','在平台发起求助前核对自动定位，补充入口、楼层和明显地标。位于商场或园区时，可说明方便进入的门和通道。向现场协作人员及时更新位置变化，保持通讯畅通。平台信息协作不能代替专业急救服务。'),
                ('health','整理体检报告，先保留日期与原始单位','把历次报告放在一起，便于后续核对和就诊沟通。','整理报告时保留检查日期、出具机构、项目名称、原始数值、单位及原报告参考区间。不同机构的检测方法与区间可能不同，避免自行改写单位或据此判断疾病。识别导入后逐项核对原图，有疑问时咨询出具报告的机构。'),
                ('exercise','出门运动前，做好这份联络准备','提前确认路线和联系人，让出行安排更从容。','出发前把路线、预计返回时间和同行人员告知可信任的人。确认手机电量、天气与道路开放情况，在平台中核对紧急联系人是否有效。路过公共服务点时，可以留意可求助的工作人员和设备位置。'),
                ('device','AED 取用结束后，怎样完成设备交接','设备归还与现场交接，也需要完整的业务记录。','查看本次任务中的设备保管状态，按照管理方说明联系交接人员。在实际完成交接后再确认归还，并说明设备外观和耗材使用情况。设备能否重新开放需由管理方按要求核实，不能只凭平台流程完成判断。'),
                ('health','给家人设置紧急联系人，记得定期核对','号码、关系和联系顺序，最好与本人确认一次。','添加紧急联系人前先征得对方同意，核对姓名、关系与号码是否正确。更换号码或家庭联络安排变化时及时更新。演练平台功能时避免拨打无关号码，真实紧急情况应通过当地专业急救渠道求助。'),
                ('emergency','救援记录里的几个时间点，分别表示什么','响应、到场和确认时间，帮助还原协作过程。','请求时间表示求助记录创建，响应时间表示志愿者接单，到场时间来自现场操作记录。提交完成与确认完成是两个不同环节，设备归还另有记录。复盘展示已有事实，缺失的时间需要核实，不能由系统推断补齐。'),
                ('device','移动 AED 与固定 AED，开放信息有什么区别','固定点位看时段，移动设备还要关注位置更新。','固定设备需要核对具体取用地点与开放时段。移动设备随携带者活动，位置有时效性，查看时应关注最近更新和在线情况。设备可用性以平台最新状态和现场核实为准，不要将旧位置当成持续有效的位置。'),
                ('health','健康数据授权，怎样按自己的需要管理','在个人设置中核对共享范围，保留必要的隐私选择。','定期查看健康数据处理授权与通知设置，确认当前选择符合自己的需要。上传检查单前遮盖不必要的个人信息，识别完成后核对保存项目。需要咨询健康问题时向专业人员提供原始报告，平台整理结果仅作信息辅助。'),
            ]
            for i, (category, title, summary, body) in enumerate(articles):
                submission, article = identifier(f'article/{i}'), 'submission-'+identifier(f'article/{i}')
                at = NOW - timedelta(days=2+i*3)
                add('science_submissions', id=submission, user_id=user if i%2 == 0 else volunteer,
                    title=title, category=category, content=body, has_cover_image=False, status='APPROVED',
                    submitted_at=at-timedelta(hours=6), reviewed_at=at, review_note='已核对内容范围与表达')
                add('science_contents', id=article, source_submission_id=submission, kind='ARTICLE',
                    category=category, title=title, summary=summary, body=body, author='云栖志愿服务队',
                    active=True, published_at=at, sort_order=10+i, view_count=0,
                    like_count=1 if i < 3 else 0, collect_count=1 if i < 3 else 0,
                    cover_path='/static/editorial/editorial-'+('aed' if category in ('device','emergency') else 'monitoring')+'-light-v1.webp')
                if i < 3:
                    add('science_article_interactions', id=identifier(f'interaction/{i}'), user_id=user,
                        article_id=article, liked=True, collected=True, updated_at=NOW-timedelta(days=i))
            for i, title in enumerate(['园区 AED 标识优化建议','邻里中心开放时段更新说明']):
                add('science_submissions', id=identifier(f'pending/{i}'), user_id=user, title=title,
                    category='device', content='近期巡查发现入口指引还可以更清晰，建议补充楼层和服务台方位，并在点位说明中同步开放时段。',
                    has_cover_image=False, status='PENDING', submitted_at=NOW-timedelta(hours=8+i*11))

            if not query(f'SELECT id FROM emergency_contacts WHERE user_id={literal(user)}'):
                for i, (name, phone, relation) in enumerate([('林悦','19900000101','家人'),('周宁','19900000102','朋友')]):
                    add('emergency_contacts', id=identifier(f'contact/{i}'), user_id=user, name=name,
                        phone=phone, relation_name=relation, created_at=NOW-timedelta(days=45-i*10))
            wearable = query(f'SELECT id FROM wearable_devices WHERE user_id={literal(user)}')
            watch = wearable[0]['ID'] if wearable else identifier('wearable')
            if not wearable:
                add('wearable_devices', id=watch, user_id=user, device_identifier='MAIAN-PREVIEW-WATCH-001',
                    name='我的健康手环', type='watch', connected=True, battery=76,
                    created_at=NOW-timedelta(days=35), updated_at=NOW-timedelta(minutes=2), last_seen_at=NOW-timedelta(minutes=2))
            # Thirty days plus a dense recent hour: variable readings without artificial live streaming.
            times = {NOW-timedelta(minutes=20*i) for i in range(30*72)}
            times.update(NOW-timedelta(minutes=i) for i in range(60))
            for at in sorted(times):
                scene = 'sleeping' if at.hour < 7 else 'exercise' if at.hour in (17,18) else 'resting'
                baseline = {'sleeping':59,'exercise':106,'resting':73}[scene]
                bpm = round(baseline + 4*math.sin(at.hour/3) + RNG.randint(-5,5))
                add('heart_rate_readings', user_id=user, wearable_id=watch, bpm=bpm, scene=scene, recorded_at=at)
            for i, days in enumerate([7, 93, 186]):
                report, at = identifier(f'report/{i}'), NOW-timedelta(days=days)
                add('health_reports', id=report, user_id=user, checkup_date=at.date().isoformat(),
                    hospital='云栖健康管理中心', analysis_source='LOCAL', risk_level='LOW', created_at=at,
                    summary='已整理本次录入的五项检查指标，建议核对原始报告中的项目名称、单位与参考区间。',
                    disclaimer='本结果仅用于健康信息整理，不能替代医生诊断。')
                for p, (name, value, unit) in enumerate([
                    ('血压',['118/76','122/78','120/80'][i],'mmHg'), ('静息心率',str(70+i*3),'次/分'),
                    ('空腹血糖',f'{4.8+i*.2:.1f}','mmol/L'),('总胆固醇',f'{4.3+i*.1:.1f}','mmol/L'),('体重',f'{66.5+i*.8:.1f}','kg')]):
                    add('health_report_indicators', report_id=report, position=p, name=name,
                        value_text=value, unit=unit, abnormal=False, reference_range='以原始报告所列参考区间为准')
                add('health_report_recommendations', report_id=report, position=0,
                    recommendation='保留原始报告，核对识别与录入结果；具体解读请咨询出具报告的专业人员。')

            backup = RUNTIME / 'backups' / f'before-{VERSION}-{NOW.strftime("%Y%m%d-%H%M%S")}.zip'
            backup.parent.mkdir(exist_ok=True)
            shell(f'BACKUP TO {literal(backup)}')
            sql_file = temporary / 'seed.sql'
            sql_file.write_text('SET AUTOCOMMIT FALSE;\n'+ '\n'.join(statements) +'\nCOMMIT;\n')
            result = subprocess.run(java + ['org.h2.tools.RunScript'] + connection + ['-script', str(sql_file)],
                                    capture_output=True, text=True)
            if result.returncode:
                raise RuntimeError('写入失败，事务已回滚：\n'+result.stderr)
            manifest.update(version=VERSION, source='fictional local preview fixtures', seededAt=NOW.isoformat(),
                            backup=str(backup.relative_to(ROOT)), inserted=dict(counts), rescueIds=rescue_ids)
            (RUNTIME / 'scenario-data.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n')
            print(json.dumps({'inserted':dict(counts), 'backup':str(backup.relative_to(ROOT))}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
