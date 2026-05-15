import { describe, it, expect } from 'vitest'

describe('API Client', () => {
  it('should have correct base URL structure', () => {
    const baseUrl = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1'
    expect(baseUrl).toContain('api/v1')
  })
})
