import React from 'react'
import { vi, describe, it, expect } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import EventNotificationsPanel from '../views/EventNotificationsPanel'

vi.mock('../lib/api', () => ({
  fetchEventNotifications: vi.fn(() => Promise.resolve({ data: [] })),
  postEventNotification: vi.fn(() => Promise.resolve({ status: 200 })),
  createThread: vi.fn(() => Promise.resolve({ data: { threadId: 11 } })),
  fetchThreadMessages: vi.fn(() => Promise.resolve({ data: [] })),
  postThreadMessage: vi.fn(() => Promise.resolve({ status: 200 }))
}))

describe('EventNotificationsPanel', () => {
  it('allows event owner to post a notification', async () => {
    const { postEventNotification, fetchEventNotifications } = await import('../lib/api')

    render(<EventNotificationsPanel eventId={42} canView={true} canPost={true} />)

    const title = screen.getByPlaceholderText(/Notification title/i)
    const message = screen.getByPlaceholderText(/Message/i)
    const post = screen.getByRole('button', { name: /post/i })

    expect(post).toBeDisabled()

    fireEvent.change(title, { target: { value: 'Event update' } })
    fireEvent.change(message, { target: { value: 'We moved rooms' } })

    expect(post).not.toBeDisabled()

    fireEvent.click(post)

    await waitFor(() => expect(postEventNotification).toHaveBeenCalled())
    // after posting, the component refetches notifications
    await waitFor(() => expect(fetchEventNotifications).toHaveBeenCalled())
  })

  it('opens discussion thread when Discuss clicked', async () => {
    const { fetchEventNotifications, createThread, fetchThreadMessages } = await import('../lib/api')
    fetchEventNotifications.mockResolvedValueOnce({ data: [{ id: 1, deliveryId: 100, title: 'Hello', message: 'World', createdAt: new Date().toISOString(), threadEnabled: true, read: false }] })

    render(<EventNotificationsPanel eventId={42} canView={true} canPost={false} />)

    await waitFor(() => screen.getByText(/Hello/))

    const discuss = screen.getByRole('button', { name: /discuss/i })
    fireEvent.click(discuss)

    await waitFor(() => expect(createThread).toHaveBeenCalled())
    await waitFor(() => expect(fetchThreadMessages).toHaveBeenCalled())
  })
})
