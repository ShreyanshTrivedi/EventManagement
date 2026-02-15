import React from 'react'
import { vi, describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import NotificationBell from '../ui/notifications/NotificationBell'

vi.mock('../lib/api', () => ({
  fetchInbox: vi.fn(() => Promise.resolve({ data: [] }))
}))

describe('NotificationBell', () => {
  it('shows unread count badge', async () => {
    const { fetchInbox } = await import('../lib/api')
    fetchInbox.mockResolvedValueOnce({ data: [ { id:1, deliveryId:5, read:false }, { id:2, deliveryId:6, read:true } ] })

    render(<NotificationBell />)

    await waitFor(() => screen.getByLabelText(/1 unread notifications|notifications/i))
    // badge should be present
    const badge = screen.getByText('1')
    expect(badge).toBeInTheDocument()
  })
})
