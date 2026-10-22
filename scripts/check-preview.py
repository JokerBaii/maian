#!/usr/bin/env python3
"""Smoke-check the isolated demo through the frontend proxy, using real JWTs."""
import json
from pathlib import Path
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parent.parent
RUNTIME = ROOT / '.cache' / 'preview'
BASE = 'http://127.0.0.1:5173'
HTTP = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def request(path, token=None, data=None, expected=200):
    headers = {'Content-Type': 'application/json', 'Accept': 'application/json'}
    if token:
        headers['Authorization'] = f'Bearer {token}'
    req = urllib.request.Request(BASE + path, headers=headers,
        data=None if data is None else json.dumps(data).encode())
    try:
        response = HTTP.open(req, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        assert response.status == expected, f'{path}: HTTP {response.status}, expected {expected}'
        result = json.load(response)
    if expected < 400:
        assert result['success'], f'{path}: request rejected'
    return result.get('data')


def main():
    manifest = json.loads((RUNTIME / 'demo.json').read_text())
    password = (RUNTIME / 'password').read_text()
    tokens = {}
    for role, key in [('USER', 'requesterPhone'), ('VOLUNTEER', 'volunteerPhone'), ('ADMIN', 'adminPhone')]:
        result = request('/api/v1/auth/login', data={'phone': manifest[key], 'password': password})
        tokens[role] = result['accessToken']
        profile = request('/api/v1/profile', tokens[role])
        assert profile['role'] == role, f'{role}: wrong account role'
    print('PASS 三种角色使用真实密码登录，前端代理正常')

    request('/api/v1/admin/users', tokens['USER'], expected=403)
    request('/api/v1/admin/users', tokens['ADMIN'])
    # An administrator is not implicitly a participant in another user’s rescue review.
    request('/api/v1/rescue-reviews/' + manifest['completedRescueId'], tokens['ADMIN'], expected=404)
    print('PASS 普通用户不可管理账户，非参与者不可读取复盘')

    call = request('/api/v1/rescue-calls/' + manifest['completedRescueId'], tokens['USER'])
    assert call['status'] == 'COMPLETED', 'missing completed demo rescue'
    assert call['aedCustodyStatus'] in ('RETURNING', 'RETURNED'), 'wrong completed AED custody'
    request('/api/v1/rescue-calls/responder-tasks/' + call['id'], tokens['VOLUNTEER'])
    print('PASS 求救者与志愿者均可访问完整演示救援')

    for _ in range(15):
        tasks = request('/api/v1/coordination-tasks/mine', tokens['USER'])
        if any(task['status'] == 'COMPLETED' and len(task['steps']) >= 4 for task in tasks):
            break
        time.sleep(1)
    else:
        raise AssertionError('resource coordination did not complete')
    print('PASS 设备故障事件触发真实后台协同，执行过程已落库')

    result = request('/api/v1/assistant', tokens['USER'], {'message': '推荐 AED 文章', 'useModel': False})
    assert any(action['kind'] == 'ARTICLE' for action in result['actions']), 'missing article cards'
    result = request('/api/v1/assistant', tokens['USER'], {'message': '隐私设置', 'useModel': False})
    assert any('settings' in action['url'] for action in result['actions']), 'missing settings navigation'
    print('PASS 助手可以检索演示文章并定位设置')

    # Exercise the canceled record so the completed record remains ready for first-time UI generation.
    path = '/api/v1/rescue-reviews/' + manifest['canceledRescueId']
    request(path, tokens['USER'], {'useModel': False})
    for _ in range(15):
        review = request(path, tokens['USER'])
        if review and review['status'] in ('COMPLETED', 'FAILED'):
            break
        time.sleep(1)
    assert review['status'] == 'COMPLETED', 'review worker did not finish'
    assert review['executionMode'] == 'RULES', 'preview must use local rules'
    assert len(review['steps']) >= 4, 'missing review tool trace'
    assert len(review['data']['timeline']) == 2, 'wrong canceled rescue timeline'
    assert all(metric.get('seconds') is None for metric in review['data']['metrics']), 'invented timing'
    assert not review['data']['feedbackPending'] and not review['data']['returnPending'], 'invented follow-ups'
    print('PASS 复盘后台执行、真实事件汇总及缺失时间处理正常')


if __name__ == '__main__':
    try:
        main()
    except (AssertionError, OSError, KeyError, ValueError, urllib.error.URLError) as error:
        raise SystemExit(f'FAIL {error}') from None
