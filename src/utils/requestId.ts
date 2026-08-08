export function createClientRequestId(
  cryptoApi: { randomUUID?: () => string } | undefined = (globalThis as any).crypto,
  now: () => number = Date.now,
  random: () => number = Math.random
) {
  return typeof cryptoApi?.randomUUID === 'function'
    ? cryptoApi.randomUUID()
    : `${now()}-${random().toString(36).slice(2, 12)}`
}
