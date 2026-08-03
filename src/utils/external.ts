export function openExternalUrl(url: string) {
  // #ifdef H5
  window.open(url, '_blank', 'noopener,noreferrer')
  // #endif

  // #ifdef APP-PLUS
  const runtime = (globalThis as any).plus?.runtime
  if (runtime) runtime.openURL(url)
  // #endif

  // #ifndef H5
  // #ifndef APP-PLUS
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: '课程链接已复制', icon: 'none' })
  })
  // #endif
  // #endif
}
