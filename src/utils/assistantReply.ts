import { Lexer, type Token, type Tokens } from 'marked'

export type ReplyNode = { type: 'text'; text: string } | { type: 'node'; name: string; attrs: Record<string,string>; children: ReplyNode[] }
const text = (value: string): ReplyNode => ({ type: 'text', text: value })
const element = (name: string, children: ReplyNode[], style = '', attrs: Record<string,string> = {}): ReplyNode =>
  ({ type: 'node', name, attrs: { ...attrs, ...(style ? { style } : {}) }, children })
const paragraphStyle = 'margin:0 0 6px;line-height:1.75;'
const codeStyle = 'font-family:monospace;font-size:12px;white-space:pre-wrap;overflow-wrap:anywhere;'

/** Convert parsed Markdown to a fixed node vocabulary, never model-authored HTML or attributes. */
function nodes(tokens: Token[], depth = 0): ReplyNode[] {
  if (depth > 20) return tokens.map(token => text(token.raw))
  const children = (token: Token) => nodes('tokens' in token && Array.isArray(token.tokens) ? token.tokens : [], depth + 1)
  return tokens.flatMap((token): ReplyNode[] => {
    switch (token.type) {
      case 'space': case 'def': return []
      case 'heading': return [element('p', children(token), 'margin:8px 0 5px;font-weight:650;font-size:14px;line-height:1.6;')]
      case 'paragraph': return [element('p', children(token), paragraphStyle)]
      case 'text': return token.tokens ? children(token) : [text(token.text)]
      case 'escape': return [text(token.text)]
      case 'strong': return [element('strong', children(token), 'font-weight:650;')]
      case 'em': return [element('em', children(token))]
      case 'del': return [element('del', children(token))]
      case 'br': return [element('br', [])]
      case 'codespan': return [element('code', [text(token.text)], codeStyle + 'background:#E8EDF3;border-radius:4px;padding:1px 4px;')]
      case 'code': return [element('div', [element('code', [text(token.text)], codeStyle)], 'margin:6px 0;padding:8px;background:#E8EDF3;border-radius:6px;overflow:auto;')]
      case 'blockquote': return [element('div', children(token), 'margin:6px 0;padding:6px 8px;background:#E8EDF3;border-radius:6px;color:#636366;')]
      case 'list': {
        const list = token as Tokens.List
        const items = list.items.map(item => element('li', [
          ...(item.task ? [text(item.checked ? '☑ ' : '☐ ')] : []), ...nodes(item.tokens, depth + 1)
        ], 'margin:3px 0;padding-left:1px;'))
        return [element(list.ordered ? 'ol' : 'ul', items, 'margin:4px 0 8px;padding-left:20px;list-style-position:outside;', list.ordered ? { start: String(list.start || 1) } : {})]
      }
      case 'table': {
        const table = token as Tokens.Table
        const row = (cells: Tokens.TableCell[], header: boolean) => element('tr', cells.map((cell, index) => element(header ? 'th' : 'td', nodes(cell.tokens, depth + 1),
          `padding:5px 6px;border:1px solid #DEE5EE;vertical-align:top;text-align:${table.align[index] || 'left'};${header ? 'font-weight:650;background:#E8EDF3;' : ''}`)))
        return [element('div', [element('table', [row(table.header, true), ...table.rows.map(cells => row(cells, false))], 'width:100%;border-collapse:collapse;table-layout:fixed;font-size:12px;overflow-wrap:anywhere;')], 'max-width:100%;margin:6px 0;overflow-x:auto;')]
      }
      // Model text cannot create business navigation, remote images or executable HTML.
      case 'link': return [element('span', children(token))]
      case 'image': return [text(token.text ? `〔图片：${token.text}〕` : '〔图片〕')]
      case 'html': return /^<br\s*\/?>$/i.test(token.raw.trim()) ? [element('br', [])] : [text(token.raw)]
      case 'hr': return [element('hr', [], 'margin:8px 0;border:0;border-top:1px solid #DEE5EE;')]
      default: return [text(token.raw)]
    }
  })
}

export function assistantReplyNodes(reply: string): ReplyNode[] {
  try { return nodes(Lexer.lex(reply, { gfm: true, breaks: true })) }
  catch { return [text(reply)] }
}
