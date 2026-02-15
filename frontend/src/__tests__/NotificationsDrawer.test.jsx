import React from 'react'
import { vi, describe, it, expect } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import NotificationsDrawer from '../ui/notifications/NotificationsDrawer'

vi.mock('../lib/api', () => ({
  fetchInbox: vi.fn(() => Promise.resolve({ data: [] })),
  markDeliveryRead: vi.fn(() => Promise.resolve({ status: 200 })),
  muteDelivery: vi.fn(() => Promise.resolve({ status: 200 })),
  createThread: vi.fn(() => Promise.resolve({ data: { threadId: 99 } })),
  fetchThreadMessages: vi.fn(() => Promise.resolve({ data: [] })),
  postThreadMessage: vi.fn(() => Promise.resolve({ status: 200 }))
}))

describe('NotificationsDrawer', () => {
  it('renders inbox and marks all as read', async () => {
    const { fetchInbox, markDeliveryRead } = await import('../lib/api')
    fetchInbox.mockResolvedValueOnce({ data: [{ id: 1, deliveryId: 200, title: 'Hey', message: 'Msg', createdAt: new Date().toISOString(), read: false, threadEnabled: false }] })

    render(<NotificationsDrawer open={true} onClose={() => {}} />)

    await waitFor(() => screen.getByText(/Hey/))

    const markAll = screen.getByRole('button', { name: /mark all/i })
    fireEvent.click(markAll)

    await waitFor(() => expect(markDeliveryRead).toHaveBeenCalled())
  })

  it('creates a thread and allows replies', async () => {
    const { fetchInbox, createThread, fetchThreadMessages } = await import('../lib/api')
    fetchInbox.mockResolvedValueOnce({ data: [{ id: 2, deliveryId: 201, title: 'Thread me', message: 'Start', createdAt: new Date().toISOString(), read: false, threadEnabled: true }] })

    render(<NotificationsDrawer open={true} onClose={() => {}} />)

    await waitFor(() => screen.getByText(/Thread me/))

    const replyBtn = screen.getByRole('button', { name: /reply/i })
    fireEvent.click(replyBtn)

    await waitFor(() => expect(createThread).toHaveBeenCalled())
    await waitFor(() => expect(fetchThreadMessages).toHaveBeenCalled())
  })
})
