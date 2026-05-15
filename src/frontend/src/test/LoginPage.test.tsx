import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { AuthProvider } from '../hooks/useAuth'
import LoginPage from '../pages/LoginPage'

describe('LoginPage', () => {
  it('renders login form with title and submit button', () => {
    render(
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    )
    expect(screen.getByText('运维管理平台')).toBeDefined()
    expect(screen.getByText('登 录')).toBeDefined()
  })
})
