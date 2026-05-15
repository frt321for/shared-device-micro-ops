import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { AuthProvider } from '../hooks/useAuth'
import RevenuePage from '../pages/RevenuePage'

describe('RevenuePage', () => {
  it('renders with title', () => {
    render(
      <AuthProvider>
        <RevenuePage />
      </AuthProvider>
    )
    expect(screen.getByText('营收分析')).toBeDefined()
  })
})
