import { onMounted, onUnmounted } from 'vue'
import { onHide, onShow } from '@dcloudio/uni-app'

/** Keep the current business page in sync with operations performed inside its assistant sheet. */
export function useAssistantRefresh(refresh: () => unknown | Promise<unknown>) {
  let visible = true
  onShow(() => { visible = true })
  onHide(() => { visible = false })
  const changed = () => { if (visible) void Promise.resolve(refresh()).catch(() => undefined) }
  onMounted(() => uni.$on('maian:agent-executed', changed))
  onUnmounted(() => uni.$off('maian:agent-executed', changed))
}
