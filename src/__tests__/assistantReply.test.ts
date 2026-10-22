import { describe, expect, it } from 'vitest'
import { assistantReplyNodes, type ReplyNode } from '../utils/assistantReply'
function flatten(nodes: ReplyNode[]): ReplyNode[] { return nodes.flatMap(node => node.type === 'node' ? [node, ...flatten(node.children)] : [node]) }
const content = (nodes: ReplyNode[]): string => flatten(nodes).filter((node): node is Extract<ReplyNode,{type:'text'}> => node.type === 'text').map(node => node.text).join('')
const tags = (nodes: ReplyNode[]) => flatten(nodes).filter((node): node is Extract<ReplyNode,{type:'node'}> => node.type === 'node')
describe('assistant Markdown replies', () => {
  it('renders the reported bold section titles and bullet groups without literal markers', () => {
    const nodes=assistantReplyNodes('**健康与安全**\n- 查看心率、健康监测数据、体检报告\n- 管理穿戴设备、个人资料\n\n**急救相关**\n- 查找附近AED')
    expect(tags(nodes).filter(node=>node.name==='strong')).toHaveLength(2)
    expect(tags(nodes).filter(node=>node.name==='ul')).toHaveLength(2)
    expect(tags(nodes).filter(node=>node.name==='li')).toHaveLength(3)
    expect(content(nodes)).toContain('健康与安全');expect(content(nodes)).not.toContain('**')
  })
  it('keeps nested ordered lists, emphasis, code and line breaks readable', () => {
    const nodes=assistantReplyNodes('### 办理流程\n3. **选择文件**\n   - *保留原件*\n4. 确认\n\n> 先核对\n\n文字\n下一行\n\n```txt\n**这里是原文**\n```')
    expect(tags(nodes).find(node=>node.name==='ol')?.attrs.start).toBe('3')
    expect(tags(nodes).some(node=>node.name==='ul')).toBe(true)
    expect(tags(nodes).some(node=>node.name==='em')).toBe(true)
    expect(tags(nodes).some(node=>node.name==='br')).toBe(true)
    expect(content(nodes)).toContain('**这里是原文**')
    expect(tags(nodes).every(node=>!node.attrs.style?.includes('border-left'))).toBe(true)
  })
  it('renders table cells as text and keeps their formatting', () => {
    const nodes=assistantReplyNodes('| 功能 | 状态 |\n| :--- | ---: |\n| **设置** | 可办理 |')
    expect(tags(nodes).filter(node=>node.name==='table')).toHaveLength(1)
    expect(tags(nodes).filter(node=>node.name==='th')).toHaveLength(2)
    expect(tags(nodes).find(node=>node.name==='td')?.attrs.style).toContain('text-align:left')
    expect(content(nodes)).toContain('设置')
  })
  it('cannot create executable HTML, clickable model URLs or remote images', () => {
    const nodes=assistantReplyNodes('<img src=x onerror="alert(1)">\n\n[打开](javascript:alert(1))\n\n![图片](https://example.com/private.png)\n\n<script>alert(1)</script>')
    expect(tags(nodes).some(node=>['script','img','a','iframe'].includes(node.name))).toBe(false)
    expect(tags(nodes).some(node=>Object.keys(node.attrs).some(key=>/^(on|src|href)/.test(key)))).toBe(false)
    expect(content(nodes)).toContain('打开');expect(content(nodes)).toContain('图片')
  })
})
