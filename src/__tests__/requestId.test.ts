import { describe, expect, it } from 'vitest'
import { createClientRequestId } from '../utils/requestId'

describe('createClientRequestId', () => {
  it('uses one cryptographically generated id for an SOS session', () => {
    expect(createClientRequestId({ randomUUID: () => 'stable-sos-id' }))
      .toBe('stable-sos-id')
  })

  it('has a deterministic fallback when randomUUID is unavailable', () => {
    expect(createClientRequestId({}, () => 1234, () => 0.5))
      .toBe('1234-i')
  })
})
