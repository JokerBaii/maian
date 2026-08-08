import { describe, expect, it } from 'vitest'
import { parseHeartRateMeasurement } from '../utils/heartRateMeasurement'

describe('BLE heart-rate parser', () => {
  it('parses 8-bit measurements', () => {
    expect(parseHeartRateMeasurement(Uint8Array.from([0, 72]).buffer)).toBe(72)
  })

  it('parses little-endian 16-bit measurements', () => {
    expect(parseHeartRateMeasurement(Uint8Array.from([1, 130, 0]).buffer)).toBe(130)
  })

  it('rejects truncated and physiologically invalid packets', () => {
    expect(parseHeartRateMeasurement(Uint8Array.from([1, 90]).buffer)).toBeNull()
    expect(parseHeartRateMeasurement(Uint8Array.from([0, 10]).buffer)).toBeNull()
  })
})
