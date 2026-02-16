import React from 'react'
import BroadcastForm from '../ui/BroadcastForm'

export default function AdminBroadcast() {
  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Send Global Notification</h1>
          <div className="text-sm text-slate-600">Broadcast a message to all users. Optionally enable a discussion thread for replies.</div>
        </div>
      </div>

      <div className="card">
        <BroadcastForm />
      </div>
    </div>
  )
}
