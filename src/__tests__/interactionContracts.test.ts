import { describe, expect, it } from 'vitest'
import pagesConfig from '../pages.json'

const pageModules = import.meta.glob('../pages/**/*.vue', {
  query: '?raw',
  import: 'default',
  eager: true
}) as Record<string, string>

const registeredPages = new Set(
  pagesConfig.pages.map((page: { path: string }) => `/${page.path}`)
)

describe('page interaction contracts', () => {
  it('keeps every literal page destination registered', () => {
    const invalidDestinations: string[] = []

    for (const [file, source] of Object.entries(pageModules)) {
      const destinations = source.match(/\/pages\/[a-zA-Z0-9_-]+\/[a-zA-Z0-9_-]+/g) || []
      for (const destination of destinations) {
        if (!registeredPages.has(destination)) {
          invalidDestinations.push(`${file}: ${destination}`)
        }
      }
    }

    expect([...new Set(invalidDestinations)]).toEqual([])
  })

  it('does not leave template tap handlers without an implementation', () => {
    const missingHandlers: string[] = []
    let bindingCount = 0

    for (const [file, source] of Object.entries(pageModules)) {
      const bindings = [...source.matchAll(/@tap(?:\.stop)?="([^"]+)"/g)]
      bindingCount += bindings.length

      for (const [, expression] of bindings) {
        const invocation = expression.trim().match(/^([a-zA-Z_$][\w$]*)\s*\(/)
        if (!invocation) continue
        const handler = invocation[1]
        const implemented = new RegExp(`function\\s+${handler}\\s*\\(|(?:const|let)\\s+${handler}\\s*=`).test(source)
        if (!implemented) missingHandlers.push(`${file}: ${handler}`)
      }
    }

    expect(bindingCount).toBeGreaterThanOrEqual(150)
    expect([...new Set(missingHandlers)]).toEqual([])
  })
})
