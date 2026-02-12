import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../lib/api'

export default function Bookings() {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/api/room-requests/mine')
      .then(res => setBookings(res.data || []))
      .catch(() => setError('Failed to load bookings'))
      .finally(() => setLoading(false))
  }, [])

  const getStatusColor = (status) => {
    switch (status) {
      case 'confirmed': return 'bg-green-100 text-green-800'
      case 'pending': return 'bg-yellow-100 text-yellow-800'
      case 'completed': return 'bg-gray-100 text-gray-800'
      default: return 'bg-gray-100 text-gray-800'
    }
  }

  const stats = (() => {
    const total = bookings.length
    const pending = bookings.filter(b => String(b.status || '').toUpperCase() === 'PENDING').length
    const approved = bookings.filter(b => String(b.status || '').toUpperCase() === 'APPROVED').length
    const confirmed = bookings.filter(b => String(b.status || '').toUpperCase() === 'CONFIRMED').length
    const allocatedRooms = new Set(bookings.map(b => b.allocatedRoom).filter(Boolean))
    let minutes = 0
    for (const b of bookings) {
      if (!b.start || !b.end) continue
      const s = new Date(b.start)
      const e = new Date(b.end)
      const diff = (e.getTime() - s.getTime()) / 60000
      if (Number.isFinite(diff) && diff > 0) minutes += diff
    }
    const hoursBooked = Math.round((minutes / 60) * 10) / 10
    return { total, pending, approved, confirmed, rooms: allocatedRooms.size, hoursBooked }
  })()

  return (
    <div>
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">My Room Requests</h1>
          <p className="text-slate-600">Track your room booking requests and their approval status</p>
        </div>
        <div className="flex gap-2">
          <Link to="/book-room" className="btn btn-primary">Book a New Room</Link>
        </div>
      </div>

      {error && (
        <div className="alert alert-error mb-6">{error}</div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div className="card">
          <div className="text-sm text-slate-600">Total</div>
          <div className="mt-1 text-3xl font-bold text-slate-900">{stats.total}</div>
        </div>
        <div className="card">
          <div className="text-sm text-slate-600">Pending</div>
          <div className="mt-1 text-3xl font-bold text-slate-900">{stats.pending}</div>
        </div>
        <div className="card">
          <div className="text-sm text-slate-600">Confirmed</div>
          <div className="mt-1 text-3xl font-bold text-slate-900">{stats.confirmed}</div>
        </div>
        <div className="card">
          <div className="text-sm text-slate-600">Hours booked</div>
          <div className="mt-1 text-3xl font-bold text-slate-900">{stats.hoursBooked}</div>
        </div>
      </div>

      {loading ? (
        <div className="card text-center py-12">
          <div className="spinner mx-auto mb-4"></div>
          <p className="text-gray-600">Loading your requests...</p>
        </div>
      ) : bookings.length === 0 ? (
        <div className="card text-center py-12">
          <div className="text-4xl mb-4">🏢</div>
          <h3 className="text-lg font-semibold mb-2">No requests yet</h3>
          <p className="text-gray-600 mb-6">Start by submitting your first room request</p>
          <Link to="/book-room" className="btn btn-primary">
            Book a Room
          </Link>
        </div>
      ) : (
        <div className="space-y-6">
          {bookings.map(booking => (
            <div key={booking.id} className="card">
              <div className="flex items-start justify-between mb-4">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">{booking.eventTitle || 'Meeting'}</h3>
                  <p className="text-gray-600 text-sm">Allocated: {booking.allocatedRoom || 'TBD'}</p>
                </div>
                <span className={`text-xs px-2 py-1 rounded-full ${getStatusColor((booking.status || '').toLowerCase())}`}>
                  {(booking.status || '').toUpperCase()}
                </span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                <div>
                  <div className="text-sm font-medium text-gray-700">Date</div>
                  <div className="text-sm text-gray-600">
                    {booking.start ? new Date(booking.start).toLocaleDateString() : '-'}
                  </div>
                </div>
                <div>
                  <div className="text-sm font-medium text-gray-700">Time</div>
                  <div className="text-sm text-gray-600">
                    {booking.start ? (
                      `${new Date(booking.start).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}${booking.end ? ` – ${new Date(booking.end).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}` : ''}`
                    ) : '-'}
                  </div>
                </div>
                <div>
                  <div className="text-sm font-medium text-gray-700">Status</div>
                  <div className="text-sm text-gray-600">{booking.status}</div>
                </div>
              </div>
              {(booking.pref1 || booking.pref2 || booking.pref3) && (
                <div className="text-xs text-gray-500">Preferences: {booking.pref1 || '—'} → {booking.pref2 || '—'} → {booking.pref3 || '—'}</div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}


