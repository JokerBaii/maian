import { describe, expect, it } from 'vitest'
import pagesConfig from '../pages.json'

const tabPages = new Set((pagesConfig.tabBar?.list || []).map((item: { pagePath: string }) => item.pagePath))
const pageModules = import.meta.glob('../pages/**/*.vue', {
  query: '?raw',
  import: 'default',
  eager: true
}) as Record<string, string>

describe('registered page reachability', () => {
  it('does not keep unreachable pages in the application bundle', () => {
    const source = Object.values(pageModules).join('\n')
    const unreachable = pagesConfig.pages
      .map((page: { path: string }) => page.path)
      .filter((pagePath: string) => !tabPages.has(pagePath))
      .filter((pagePath: string) => !source.includes(`/${pagePath}`))

    expect(unreachable).toEqual([])
  })
})
