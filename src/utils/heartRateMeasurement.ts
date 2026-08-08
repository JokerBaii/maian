/** Bluetooth SIG Heart Rate Measurement (0x2A37) 的 8/16 位心率解析。 */
export function parseHeartRateMeasurement(value: ArrayBuffer): number | null {
  const view = new DataView(value)
  if (view.byteLength < 2) return null
  const usesSixteenBits = (view.getUint8(0) & 0x01) === 0x01
  if (usesSixteenBits && view.byteLength < 3) return null
  const bpm = usesSixteenBits ? view.getUint16(1, true) : view.getUint8(1)
  return bpm >= 25 && bpm <= 250 ? bpm : null
}
