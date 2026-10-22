#!/usr/bin/env python3
"""Exercise real HTTP workflows using separate accounts in the local preview database."""
import datetime
import json
from pathlib import Path
import secrets
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parent.parent
BASE = 'http://127.0.0.1:5173/api/v1'
HTTP = urllib.request.build_opener(urllib.request.ProxyHandler({}))
PASSED = []


def api(method, path, token=None, data=None, expected=200):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + path, method=method, headers=headers,
        data=None if data is None else json.dumps(data).encode())
    try:
        response = HTTP.open(req, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read()
        result = json.loads(raw) if raw else {}
        assert response.status == expected, f'{method} {path}: {response.status}, expected {expected}: {result.get("error")}'
        return result.get('data')


def passed(message):
    PASSED.append(message)
    print('PASS ' + message, flush=True)


def report(token, identifier):
    api('POST', '/rescue-reviews/' + identifier, token, {'useModel': False})
    for _ in range(40):
        value = api('GET', '/rescue-reviews/' + identifier, token)
        if value and value['status'] in ('COMPLETED', 'FAILED'):
            assert value['status'] == 'COMPLETED', value['summary']
            return value
        time.sleep(.25)
    raise AssertionError('review worker timed out')


def main():
    assert (ROOT / '.cache/preview/demo.json').exists(), 'Only run against the local preview environment'
    admin = api('POST', '/auth/login', data={'phone': '19900000003',
        'password': (ROOT / '.cache/preview/password').read_text()})['accessToken']
    assert api('GET', '/profile', admin)['role'] == 'ADMIN'
    password = 'Qa-' + secrets.token_hex(12)
    suffix = int(time.time_ns() // 1000000) % 99999900
    accounts = []
    for index in range(3):
        phone = '198' + f'{suffix + index:08d}'
        token = api('POST', '/auth/register', data={'phone': phone, 'password': password, 'nickname': '【自测】角色' + str(index)}, expected=201)['accessToken']
        accounts.append((phone, token, api('GET', '/profile', token)['id']))
    user, volunteer, outsider = (account[1] for account in accounts)
    api('PATCH', '/admin/users/' + accounts[1][2] + '/role', admin, {'role': 'VOLUNTEER'})
    volunteer = api('POST', '/auth/login', data={'phone': accounts[1][0], 'password': password})['accessToken']
    api('GET', '/admin/users', user, expected=403)
    api('POST', '/assistant', user, {'message': '', 'useModel': False}, expected=400)
    passed('注册、登录、角色授权、管理权限与非法输入校验')

    settings = api('GET', '/settings', user)
    settings.update({'healthDataShare': False, 'maxHeartRate': 130, 'minHeartRate': 50, 'scienceUpdate': False})
    api('PUT', '/settings', user, settings)
    assert api('GET', '/settings', user)['maxHeartRate'] == 130
    bad = dict(settings, minHeartRate=150)
    api('PUT', '/settings', user, bad, expected=400)
    contact = api('POST', '/emergency-contacts', user, {'name': '【自测】联系人', 'phone': accounts[1][0], 'relation': '同学'}, expected=201)
    api('PUT', '/emergency-contacts/' + contact['id'], outsider, {'name': '越权', 'phone': accounts[1][0], 'relation': '同学'}, expected=404)
    api('DELETE', '/emergency-contacts/' + contact['id'], user)
    passed('设置读写与阈值校验、联系人增删及跨账号隔离')

    api('PUT', '/wearable-device', user, {'deviceIdentifier': 'qa-' + uuid.uuid4().hex, 'name': '【自测】模拟穿戴输入', 'type': 'watch', 'connected': True, 'battery': 90})
    for seconds in [40, 20, 1]:
        recorded_at = (datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(seconds=seconds)).isoformat()
        api('POST', '/heart-rate-readings', user, {'bpm': 150, 'scene': 'resting', 'recordedAt': recorded_at}, expected=201)
    monitoring = api('GET', '/health-monitoring', user)
    assert monitoring['current'] == 150 and monitoring['status'] == 'high'
    assert monitoring['alerts'], 'missing high-heart-rate alert'
    api('POST', '/heart-rate-readings', user, {'bpm': 999, 'scene': 'resting'}, expected=400)
    health = api('POST', '/health-reports', user, {'checkupDate': datetime.date.today().isoformat(), 'hospital': '【自测】录入机构',
        'indicators': [{'name': '血压', 'value': '120/80', 'unit': 'mmHg', 'referenceRange': '样例参考', 'abnormal': False}]}, expected=201)
    api('GET', '/health-reports/' + health['id'], outsider, expected=404)
    api('DELETE', '/health-reports/' + health['id'], user, expected=204)
    api('DELETE', '/wearable-device', user, expected=204)
    passed('模拟穿戴输入、心率预警、体检手动录入及健康数据权限')

    submission = api('POST', '/science-submissions', user, {'title': '【自测】AED 功能说明', 'category': 'device', 'content': '这是软件功能回归测试资料，不包含医学指导。'}, expected=201)
    try:
        api('PATCH', '/science-submissions/' + submission['id'] + '/review', user, {'approved': True}, expected=403)
        api('PATCH', '/science-submissions/' + submission['id'] + '/review', admin, {'approved': True, 'reviewNote': '本地功能回归'})
        article_id = 'submission-' + submission['id']
        assert api('GET', '/science-content/' + article_id, user)['title'] == '【自测】AED 功能说明'
        interaction = api('PUT', '/science-articles/' + article_id + '/interaction', user, {'liked': True, 'collected': True})
        assert interaction['liked'] and interaction['collected']
    finally:
        # Published fixtures otherwise displace the demo articles in recommendations on every run.
        api('DELETE', '/science-submissions/' + submission['id'], user, expected=204)
    passed('科普投稿、管理员审核、文章发布与点赞收藏')

    # A separate location grid avoids matching demo AEDs or fixtures from a previous run.
    latitude = 32 + (suffix % 17) * .08
    longitude = 111 + (suffix % 29) * .08
    windows = [{'dayOfWeek': day, 'opensAt': '00:00', 'closesAt': '00:00'} for day in ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']]
    devices = []
    for index in range(2):
        device = api('POST', '/emergency-devices', user, {'type': 'FIXED', 'category': 'AED', 'name': '【自测】AED-' + str(index),
            'address': '【自测】虚构设备位置', 'latitude': latitude, 'longitude': longitude + index * .0001,
            'serviceWindows': windows}, expected=201)
        api('PATCH', '/emergency-devices/' + device['id'] + '/review', admin, {'approved': True, 'reviewNote': '本地功能回归'})
        devices.append(device['id'])
    api('PUT', '/rescue-calls/responder-presence', volunteer, {'latitude': latitude, 'longitude': longitude, 'available': True})
    payload = {'urgency': 'CRITICAL', 'latitude': latitude, 'longitude': longitude, 'address': '【自测】虚构呼救现场',
        'description': '软件功能回归，请勿用于真实救援', 'symptoms': ['【自测】流程验证'], 'clientRequestId': uuid.uuid4().hex}
    call = api('POST', '/rescue-calls', user, payload, expected=201)
    identifier = call['id']
    path = '/rescue-calls/' + identifier
    assert call['matchedAed']['deviceId'] in devices
    assert api('POST', '/rescue-calls', user, payload, expected=201)['id'] == identifier
    offer = next(item for item in api('GET', '/rescue-calls/responder-tasks', volunteer)['content'] if item['id'] == identifier)
    assert not offer.get('address') and not offer.get('requester'), 'offer leaked participant data'
    api('POST', path + '/acceptance', volunteer)
    api('GET', path, outsider, expected=404)
    source = call['matchedAed']['deviceId']
    api('POST', '/emergency-devices/' + source + '/fault', user)
    api('POST', path + '/aed-replacement', outsider, {'expectedDeviceId': source}, expected=403)
    replaced = api('POST', path + '/aed-replacement', volunteer, {'expectedDeviceId': source})
    assert replaced['matchedAed']['deviceId'] != source
    passed('设备登记审核、呼救幂等、接单隐私、故障 AED 人工更换')

    for action in ['aed-arrival', 'aed-pickup', 'requester-arrival', 'rescue-start', 'completion-submission']:
        api('POST', path + '/' + action, volunteer)
    api('POST', path + '/completion-confirmation', user)
    review = report(user, identifier)
    assert review['data']['returnPending'] and review['data']['feedbackPending']
    assert len(review['data']['timeline']) >= 10 and len(review['steps']) >= 4
    api('POST', path + '/aed-return', volunteer)
    assert api('GET', '/rescue-reviews/' + identifier, user)['stale']
    api('POST', path + '/feedback', user, {'rating': 5, 'comment': '【自测】流程评价'}, expected=201)
    review = report(user, identifier)
    assert not review['data']['returnPending'] and not review['data']['feedbackPending']
    assert not review['stale']
    api('GET', '/rescue-reviews/' + identifier, outsider, expected=404)
    # Preserve the rescue history; retire test AEDs rather than deleting referenced rows.
    for device_id in devices:
        device = api('GET', '/emergency-devices/mine/' + device_id, user)
        if device['status'] == 'AVAILABLE':
            api('POST', '/emergency-devices/' + device_id + '/disable', user)
    passed('取用→到场→施救→完成→复盘→归还→评价→更新复盘完整闭环')


if __name__ == '__main__':
    try:
        main()
    finally:
        output = ROOT / '.cache/preview/qa'
        output.mkdir(parents=True, exist_ok=True)
        (output / 'business-flow-results.json').write_text(json.dumps({'passed': PASSED}, ensure_ascii=False, indent=2))
