import React, { useEffect, useState } from 'react'

export default function Toast() {
  const [toasts, setToasts] = useState([])

  useEffect(() => {
    const handler = (e) => {
      const t = { id: e.detail.id || Date.now(), message: e.detail.message, type: e.detail.type || 'info', timeout: e.detail.timeout || 4000 }
      setToasts((s) => [t, ...s])
      setTimeout(() => setToasts((s) => s.filter(x => x.id !== t.id)), t.timeout)
    }
    window.addEventListener('app-toast', handler)
    return () => window.removeEventListener('app-toast', handler)
  }, [])

  const dismiss = (id) => setToasts((s) => s.filter(t => t.id !== id))

  if (!toasts.length) return null

  return (
    <div className="toast-container" aria-live="polite" aria-atomic="true">
      {toasts.map((t) => (
        <div key={t.id} className={`toast ${t.type}`} role="status">
          <div className="flex-1">
            <div className="toast-title">{t.message}</div>
          </div>
          <button className="btn btn-ghost btn-sm toast-close" onClick={() => dismiss(t.id)} aria-label="Dismiss notification">✕</button>
        </div>
      ))}
    </div>
  )
}
