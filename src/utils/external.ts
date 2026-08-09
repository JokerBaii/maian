export function openExternalUrl(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')

  const runtime = (globalThis as any).plus?.runtime
  if (runtime) runtime.openURL(url)

  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: '课程链接已复制', icon: 'none' })
  })
}
