import React from 'react'
import { vi, expect, describe, it } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import NotificationCard from '../ui/notifications/NotificationCard'

const sample = {
  id: 1,
  deliveryId: 101,
  title: 'Test notice',
  message: 'Hello world',
  createdAt: new Date().toISOString(),
  urgency: 'NORMAL',
  threadEnabled: true,
  read: false,
  muted: false
}

describe('NotificationCard', () => {
  it('renders and calls handlers', () => {
    const onMarkRead = vi.fn()
    const onReply = vi.fn()
    const onMute = vi.fn()

    render(<NotificationCard item={sample} onMarkRead={onMarkRead} onReply={onReply} onMute={onMute} />)

    expect(screen.queryByText(/Test notice/)).not.toBeNull()
    const replyBtn = screen.getByRole('button', { name: /reply/i })
    fireEvent.click(replyBtn)
    expect(onReply).toHaveBeenCalled()

    const markBtn = screen.getByRole('button', { name: /mark read/i })
    fireEvent.click(markBtn)
    expect(onMarkRead).toHaveBeenCalled()

    const muteBtn = screen.getByRole('button', { name: /mute/i })
    fireEvent.click(muteBtn)
    expect(onMute).toHaveBeenCalled()
  })
})