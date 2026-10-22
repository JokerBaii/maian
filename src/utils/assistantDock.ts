export interface DockBounds { left: number; right: number; top: number; bottom: number }
export interface DockPosition { x: number; y: number }
export interface SavedDock { side: 'left' | 'right'; fraction: number }
export interface AssistantWindow extends DockPosition { width: number; height: number }

export function assistantWindowBounds(width: number, height: number, dock: DockPosition, tabPage: boolean, safeTop = 0, safeBottom = 0): AssistantWindow {
  const contentWidth = width >= 768 ? 430 : width
  const left = (width - contentWidth) / 2 + 12
  const top = Math.max(56, safeTop + 48)
  const bottom = Math.max(top + 80, height - safeBottom - (tabPage ? 64 : 12))
  const windowWidth = Math.max(80, Math.min(320, contentWidth - 24))
  const above = Math.max(0, dock.y - 12 - top)
  const below = Math.max(0, bottom - dock.y - 108)
  const available = Math.max(above, below)
  const windowHeight = Math.min(360, Math.round(height * .58), bottom - top, available >= 220 ? available : bottom - top)
  const x = Math.max(left, Math.min(left + contentWidth - 24 - windowWidth, dock.x < width / 2 ? dock.x : dock.x + 72 - windowWidth))
  const desiredY = above >= windowHeight ? dock.y - 12 - windowHeight : below >= windowHeight ? dock.y + 108 : dock.y - windowHeight / 2 + 48
  return { x, y: Math.max(top, Math.min(bottom - windowHeight, desiredY)), width: windowWidth, height: windowHeight }
}

export function assistantDockBounds(width: number, height: number, tabPage: boolean, safeTop = 0, safeBottom = 0): DockBounds {
  const contentWidth = width >= 768 ? 430 : width
  const left = (width - contentWidth) / 2 + 12
  const top = Math.max(76, safeTop + 54)
  return { left, right: Math.max(left, left + contentWidth - 96), top,
    bottom: Math.max(top, height - 112 - safeBottom - (tabPage ? 56 : 0)) }
}

export function clampAssistantDock(position: DockPosition, bounds: DockBounds): DockPosition {
  return { x: Math.max(bounds.left, Math.min(bounds.right, position.x)), y: Math.max(bounds.top, Math.min(bounds.bottom, position.y)) }
}

export function readAssistantDock(value: unknown): SavedDock {
  const saved = value as Partial<SavedDock> | null
  return saved && (saved.side === 'left' || saved.side === 'right') && typeof saved.fraction === 'number' && Number.isFinite(saved.fraction)
    ? { side: saved.side, fraction: Math.max(0, Math.min(1, saved.fraction)) }
    : { side: 'right', fraction: .76 }
}

export function restoreAssistantDock(saved: SavedDock, bounds: DockBounds): DockPosition {
  return { x: saved.side === 'left' ? bounds.left : bounds.right, y: bounds.top + saved.fraction * (bounds.bottom - bounds.top) }
}

export function snapAssistantDock(position: DockPosition, bounds: DockBounds): SavedDock {
  return { side: position.x < (bounds.left + bounds.right) / 2 ? 'left' : 'right',
    fraction: bounds.bottom === bounds.top ? 0 : Math.max(0, Math.min(1, (position.y - bounds.top) / (bounds.bottom - bounds.top))) }
}
