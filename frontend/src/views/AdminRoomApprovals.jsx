import React, { useEffect, useMemo, useState } from 'react'
import api from '../lib/api'
import { useAuth } from '../lib/AuthContext'

export default function AdminRoomApprovals() {
  const { hasRole } = useAuth()
  const [requests, setRequests] = useState([])
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [alloc, setAlloc] = useState({})

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const [reqRes, roomsRes] = await Promise.all([
        api.get('/api/admin/room-requests?status=PENDING'),
        api.get('/api/rooms')
      ])
      setRequests(Array.isArray(reqRes.data) ? reqRes.data : [])
      setRooms(Array.isArray(roomsRes.data) ? roomsRes.data : [])
    } catch (e) {
      setError('Failed to load data')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const approve = async (id, allocatedRoomId) => {
    if (!allocatedRoomId) { setError('Select a room to allocate'); return }
    setError('')
    try {
      await api.post(`/api/admin/room-requests/${id}/approve`, { allocatedRoomId })
      setRequests(prev => prev.filter(r => r.id !== id))
      setAlloc(prev => {
        const next = { ...prev }
        delete next[id]
        return next
      })
    } catch (e) {
      setError('Approve failed')
    }
  }

  const reject = async (id) => {
    setError('')
    try {
      await api.post(`/api/admin/room-requests/${id}/reject`)
      setRequests(prev => prev.filter(r => r.id !== id))
      setAlloc(prev => {
        const next = { ...prev }
        delete next[id]
        return next
      })
    } catch (e) {
      setError('Reject failed')
    }
  }

  if (!hasRole('ADMIN')) return null

  return (
    <div>
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Room Booking Approvals</h1>
          <p className="text-slate-600">Approve or reject pending room booking requests. Approval allocates a room; confirmation occurs automatically 2 days before the event.</p>
        </div>
        <button type="button" className="btn btn-secondary" onClick={load} disabled={loading}>Refresh</button>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}

      {loading ? (
        <div className="card text-center py-12">
          <div className="spinner mx-auto mb-4"></div>
          <div className="text-slate-600">Loading pending requests...</div>
        </div>
      ) : requests.length === 0 ? (
        <div className="card text-center py-12">
          <div className="text-4xl mb-4">✅</div>
          <div className="text-lg font-semibold text-slate-900">No pending requests</div>
          <div className="text-slate-600 mt-1">All caught up.</div>
        </div>
      ) : (
        <div className="space-y-4">
          {requests.map(req => (
            <div key={req.id} className="card">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div>
                  <div className="text-lg font-semibold text-slate-900">{req.eventTitle}</div>
                  <div className="mt-1 text-sm text-slate-600">Starts: {new Date(req.start).toLocaleString()}</div>
                  <div className="text-sm text-slate-600">Requested by: {req.requestedBy}</div>
                  <div className="text-sm text-slate-600 mt-2">Preferences: {req.pref1} → {req.pref2} → {req.pref3}</div>
                </div>
                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
                  <select
                    className="form-select"
                    value={alloc[req.id] || ''}
                    onChange={(e) => setAlloc(prev => ({ ...prev, [req.id]: e.target.value }))}
                  >
                    <option value="" disabled>Allocate room...</option>
                    {rooms.map(r => (
                      <option key={r.id} value={r.id}>{r.name} ({r.capacity || 0})</option>
                    ))}
                  </select>
                  <button className="btn btn-primary btn-sm" onClick={() => approve(req.id, Number(alloc[req.id]))}>Approve</button>
                  <button className="btn btn-secondary btn-sm" onClick={() => reject(req.id)}>Reject</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
