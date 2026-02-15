import React, { useEffect } from 'react'
import BroadcastForm from './BroadcastForm'

export default function BroadcastModal({ open, onClose }) {
  useEffect(() => {
    if (!open) return
    const onKey = (e) => { if (e.key === 'Escape') onClose && onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-xl p-4">
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <div className="text-lg font-semibold">Send Global Broadcast</div>
            <button className="btn btn-ghost" onClick={onClose} aria-label="Close broadcast dialog">✕</button>
          </div>
          <BroadcastForm onSent={onClose} />
        </div>
      </div>
    </div>
  )
}
