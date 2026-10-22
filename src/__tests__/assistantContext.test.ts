import { describe,expect,it } from 'vitest'
import { pageAssistantContext,ordinalIndex } from '../utils/assistantContext'

describe('business page context',()=>{
  it('copies only recognized object references and omits unrelated page data',()=>{
    expect(pageAssistantContext('/pages/checkup/report',{id:'report',token:'secret'})).toEqual({page:'pages/checkup/report',reportId:'report'})
    expect(pageAssistantContext('pages/device/manage',{}, {deviceId:'selected'})).toEqual({page:'pages/device/manage',deviceId:'selected'})
    expect(pageAssistantContext('pages/science/detail',{id:'article'})).toEqual({page:'pages/science/detail',articleId:'article'})
  })
  it('resolves user-visible ordinals without treating arbitrary numbers as selection',()=>{
    expect(ordinalIndex('停用第二台设备')).toBe(1);expect(ordinalIndex('取消第3条提醒')).toBe(2)
    expect(ordinalIndex('手机号13800000000')).toBeUndefined()
  })
})
