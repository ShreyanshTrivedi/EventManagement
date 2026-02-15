import React from 'react'
import { vi, expect, describe, it } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import BroadcastForm from '../ui/BroadcastForm'

vi.mock('../lib/api', () => ({
  broadcastNotification: vi.fn(() => Promise.resolve({ status: 200 }))
}))

describe('BroadcastForm', () => {
  it('validates inputs and sends broadcast', async () => {
    const mockOnSent = vi.fn()
    const { broadcastNotification } = await import('../lib/api')

    // spy on window.dispatchEvent for toast
    const dispatchSpy = vi.spyOn(window, 'dispatchEvent')

    render(<BroadcastForm onSent={mockOnSent} />)

    const title = screen.getByLabelText(/Title/i)
    const message = screen.getByLabelText(/Message/i)
    const send = screen.getByRole('button', { name: /send/i })

    // initially button disabled because fields empty
    expect(send).toBeDisabled()

    // fill form
    fireEvent.change(title, { target: { value: 'System maintenance' } })
    fireEvent.change(message, { target: { value: 'Scheduled maintenance at 2 AM' } })

    expect(send).not.toBeDisabled()

    fireEvent.click(send)

    await waitFor(() => expect(broadcastNotification).toHaveBeenCalled())
    await waitFor(() => expect(mockOnSent).toHaveBeenCalled())
    expect(dispatchSpy).toHaveBeenCalled()

    dispatchSpy.mockRestore()
  })
})