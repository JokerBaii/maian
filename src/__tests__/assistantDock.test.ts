import { describe, expect, it } from 'vitest'
import { assistantDockBounds, assistantWindowBounds, clampAssistantDock, readAssistantDock, restoreAssistantDock, snapAssistantDock } from '../utils/assistantDock'

describe('floating assistant placement', () => {
  it('keeps a dragged character inside the phone viewport and above tab controls', () => {
    const bounds = assistantDockBounds(390, 844, true, 20, 34)
    expect(clampAssistantDock({ x: -200, y: 2000 }, bounds)).toEqual({ x: 12, y: 642 })
    expect(clampAssistantDock({ x: 2000, y: -100 }, bounds)).toEqual({ x: 306, y: 76 })
  })
  it('keeps the character inside the centered phone on a desktop', () => {
    const bounds = assistantDockBounds(1280, 900, true)
    expect(bounds.left).toBe(437)
    expect(bounds.right + 72).toBe(843)
    expect(snapAssistantDock({ x: 440, y: 300 }, bounds).side).toBe('left')
  })
  it('restores a saved edge and relative height after rotating or changing pages', () => {
    const original = assistantDockBounds(390, 844, true)
    const saved = snapAssistantDock({ x: 20, y: 400 }, original)
    const rotated = assistantDockBounds(844, 390, false)
    const restored = restoreAssistantDock(saved, rotated)
    expect(restored.x).toBe(rotated.left)
    expect(restored.y).toBeGreaterThanOrEqual(rotated.top)
    expect(restored.y).toBeLessThanOrEqual(rotated.bottom)
  })
  it('ignores malformed stored coordinates and handles a very short viewport', () => {
    for (const value of [null, {}, 'x', { side: 'right', fraction: NaN }, { side: 'left', fraction: Infinity }]) {
      expect(readAssistantDock(value)).toEqual({ side: 'right', fraction: .76 })
    }
    expect(readAssistantDock({ side: 'left', fraction: 9 }).fraction).toBe(1)
    const bounds = assistantDockBounds(320, 100, true)
    expect(snapAssistantDock({ x: 12, y: 76 }, bounds).fraction).toBe(0)
  })
  it('anchors a compact conversation above the mascot without covering the whole phone', () => {
    const dock = restoreAssistantDock(readAssistantDock(null), assistantDockBounds(390,844,true))
    const window = assistantWindowBounds(390,844,dock,true)
    expect(window.width).toBe(320); expect(window.height).toBe(360)
    expect(window.y + window.height).toBeLessThan(dock.y)
    expect(window.x).toBeGreaterThanOrEqual(12); expect(window.x + window.width).toBeLessThanOrEqual(378)
  })
  it('keeps both edge anchors inside the centered desktop preview', () => {
    for (const x of [437,771]) {
      const window = assistantWindowBounds(1280,900,{x,y:574},true)
      expect(window.x).toBeGreaterThanOrEqual(437); expect(window.x + window.width).toBeLessThanOrEqual(843)
      expect(window.height).toBeLessThanOrEqual(360)
    }
  })
  it('shrinks the window to the available space on small screens and after keyboard resizing', () => {
    for (const height of [568,390,280]) {
      const dock = restoreAssistantDock(readAssistantDock(null), assistantDockBounds(320,height,true))
      const window = assistantWindowBounds(320,height,dock,true)
      expect(window.x).toBeGreaterThanOrEqual(12); expect(window.x + window.width).toBeLessThanOrEqual(308)
      expect(window.y).toBeGreaterThanOrEqual(56); expect(window.y + window.height).toBeLessThanOrEqual(height-64)
    }
  })
})
