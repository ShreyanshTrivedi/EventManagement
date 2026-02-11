import React, { useState, useEffect } from 'react'
import api from '../lib/api'
import { useAuth } from '../lib/AuthContext'

export default function RoomBooking() {
  const { hasRole } = useAuth()
  const isFaculty = hasRole('FACULTY')
  const [rooms, setRooms] = useState([])
  const [events, setEvents] = useState([])
  const [eventId, setEventId] = useState('')
  const [pref1, setPref1] = useState('')
  const [pref2, setPref2] = useState('')
  const [pref3, setPref3] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [mode, setMode] = useState('event') // 'event' or 'meeting'
  const [meetingStart, setMeetingStart] = useState('')
  const [meetingEnd, setMeetingEnd] = useState('')
  const [meetingPurpose, setMeetingPurpose] = useState('')
  const [roomConflicts, setRoomConflicts] = useState([])

  useEffect(() => {
    api.get('/api/rooms').then(res => setRooms(res.data || [])).catch(() => setRooms([]))
    api.get('/api/events/mine').then(res => setEvents(res.data || [])).catch(() => setEvents([]))
  }, [])

  const refreshAvailabilityForEvent = (evId) => {
    const ev = events.find(e => e.id === Number(evId))
    if (!ev) return
    const start = ev.startTime
    const end = ev.endTime
    if (!start || !end) return
    // Refresh room list (availability per-room can be checked on demand)
    api.get('/api/rooms')
      .then(res => setRooms(res.data || []))
      .catch(() => {})
  }

  const refreshAvailabilityForMeeting = () => {
    if (!meetingStart || !meetingEnd) return
    const start = new Date(meetingStart).toISOString().slice(0,19)
    const end = new Date(meetingEnd).toISOString().slice(0,19)
    // Refresh room list (availability per-room can be checked on demand)
    api.get('/api/rooms')
      .then(res => setRooms(res.data || []))
      .catch(() => {})
  }

  const loadRoomConflicts = (roomId) => {
    let start, end
    if (mode === 'event') {
      const ev = events.find(e => e.id === Number(eventId))
      if (!ev || !ev.startTime || !ev.endTime) return
      start = ev.startTime
      end = ev.endTime
    } else {
      if (!meetingStart || !meetingEnd) return
      start = new Date(meetingStart).toISOString().slice(0,19)
      end = new Date(meetingEnd).toISOString().slice(0,19)
    }
    api.get(`/api/rooms/${roomId}/availability`, { params: { start, end } })
      .then(res => {
        const available = !!(res.data && (res.data.available === true || res.data.available === 'true'))
        setRoomConflicts([{
          id: `availability-${roomId}`,
          title: available ? 'This room is AVAILABLE in the selected window' : 'This room is OCCUPIED in the selected window',
          start: null,
          end: null,
          status: available ? 'AVAILABLE' : 'OCCUPIED'
        }])
      })
      .catch(() => setRoomConflicts([]))
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setMessage('')
    setLoading(true)
    
    try {
      if (isFaculty) {
        if (!pref1) {
          setMessage('Please select a room')
          return
        }
        let payload
        if (mode === 'event') {
          if (!eventId) { setMessage('Please select an event'); return }
          payload = { eventId: Number(eventId), roomId: Number(pref1) }
        } else {
          if (!meetingStart || !meetingEnd || !meetingPurpose) { setMessage('Please provide meeting start, end, and purpose'); return }
          payload = {
            roomId: Number(pref1),
            start: new Date(meetingStart).toISOString().slice(0,19),
            end: new Date(meetingEnd).toISOString().slice(0,19),
            purpose: meetingPurpose
          }
        }
        const res = await api.post('/api/faculty/bookings', payload)
        if (res.status === 200) {
          setMessage('Booked and approved immediately for the selected room.')
          setEventId(''); setPref1(''); setPref2(''); setPref3('')
          setMeetingStart(''); setMeetingEnd(''); setMeetingPurpose('')
          setRoomConflicts([])
        } else {
          setMessage('Booking failed')
        }
      } else {
        if (!pref1 || !pref2 || !pref3) {
          setMessage('Please select three room preferences')
          return
        }
        if (new Set([pref1, pref2, pref3]).size < 3) {
          setMessage('Please select three different rooms')
          return
        }
        const payload = {
          pref1RoomId: Number(pref1),
          pref2RoomId: Number(pref2),
          pref3RoomId: Number(pref3)
        }
        if (mode === 'event') {
          if (!eventId) { setMessage('Please select an event'); return }
          payload.eventId = Number(eventId)
        } else {
          if (!meetingStart || !meetingEnd || !meetingPurpose) { setMessage('Please provide meeting start, end, and purpose'); return }
          payload.meetingStart = new Date(meetingStart).toISOString().slice(0,19)
          payload.meetingEnd = new Date(meetingEnd).toISOString().slice(0,19)
          payload.meetingPurpose = meetingPurpose
        }
        const res = await api.post('/api/room-requests', payload)
        if (res.status === 200) {
          setMessage('Request submitted. Admin approval pending. Official confirmation 2 days before the event/meeting.')
          setEventId(''); setPref1(''); setPref2(''); setPref3('')
          setMeetingStart(''); setMeetingEnd(''); setMeetingPurpose('')
          setRoomConflicts([])
        } else {
          setMessage('Request failed')
        }
      }
    } catch (err) {
      const data = err.response?.data
      const detail = (data && (data.error || data.message)) ? (data.error || data.message)
        : (typeof data === 'string' ? data : err.message)
      setMessage('Booking failed: ' + detail)
    } finally {
      setLoading(false)
    }
  }

  const selectedRoomInfo = rooms.find(room => room.id === Number(pref1))

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Book a Room</h1>
        <p className="text-gray-600">Reserve conference rooms, lecture halls, and meeting spaces</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Booking Form */}
        <div className="lg:col-span-2">
          <div className="card">
            <h2 className="text-xl font-semibold mb-4">Room Reservation</h2>

            <div className="flex space-x-4 mb-4">
              <button type="button" className={`btn btn-sm ${mode === 'event' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setMode('event')}>
                For Event
              </button>
              <button type="button" className={`btn btn-sm ${mode === 'meeting' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setMode('meeting')}>
                For Meeting
              </button>
            </div>
            
            <form onSubmit={onSubmit} className="space-y-6">
              {mode === 'event' ? (
                <div className="form-group">
                  <label className="form-label">Select Event</label>
                  <select
                    className="form-select"
                    value={eventId}
                    onChange={(e)=>{
                      const v = e.target.value
                      setEventId(v)
                      setMessage('')
                      if (v) refreshAvailabilityForEvent(v)
                    }}
                    required
                  >
                    <option value="">Choose one of your events...</option>
                    {events.map(ev => (
                      <option key={ev.id} value={ev.id}>{ev.title}</option>
                    ))}
                  </select>
                </div>
              ) : (
                <>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="form-group">
                      <label className="form-label">Meeting Start</label>
                      <input
                        type="datetime-local"
                        className="form-input"
                        value={meetingStart}
                        onChange={e => { setMeetingStart(e.target.value); setMessage(''); }}
                        onBlur={refreshAvailabilityForMeeting}
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Meeting End</label>
                      <input
                        type="datetime-local"
                        className="form-input"
                        value={meetingEnd}
                        onChange={e => { setMeetingEnd(e.target.value); setMessage(''); }}
                        onBlur={refreshAvailabilityForMeeting}
                      />
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Meeting Purpose</label>
                    <textarea
                      className="form-input"
                      rows={2}
                      value={meetingPurpose}
                      onChange={e => setMeetingPurpose(e.target.value)}
                      placeholder="e.g., Project review, committee meeting"
                    />
                  </div>
                </>
              )}

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="form-group">
                  <label className="form-label">Preference 1</label>
                  <select
                    className="form-select"
                    value={pref1}
                    onChange={(e)=>{
                      const v = e.target.value
                      setPref1(v)
                      setRoomConflicts([])
                      if (v) loadRoomConflicts(Number(v))
                    }}
                    required
                  >
                    <option value="">Choose a room...</option>
                    {rooms.map(room => (
                      <option key={room.id} value={room.id}>{room.name} - {room.location} (Capacity: {room.capacity})</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Preference 2</label>
                  <select className="form-select" value={pref2} onChange={(e)=>setPref2(e.target.value)} required>
                    <option value="">Choose a room...</option>
                    {rooms.map(room => (
                      <option key={room.id} value={room.id}>{room.name} - {room.location} (Capacity: {room.capacity})</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Preference 3</label>
                  <select className="form-select" value={pref3} onChange={(e)=>setPref3(e.target.value)} required>
                    <option value="">Choose a room...</option>
                    {rooms.map(room => (
                      <option key={room.id} value={room.id}>{room.name} - {room.location} (Capacity: {room.capacity})</option>
                    ))}
                  </select>
                </div>
              </div>

              {selectedRoomInfo && (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <h3 className="font-semibold text-blue-900 mb-2">Room Details</h3>
                  <div className="text-sm text-blue-800 space-y-1">
                    <div><strong>Name:</strong> {selectedRoomInfo.name}</div>
                    <div><strong>Location:</strong> {selectedRoomInfo.location}</div>
                    <div><strong>Capacity:</strong> {selectedRoomInfo.capacity} people</div>
                  </div>
                </div>
              )}

              {message && (
                <div className={`alert ${message.includes('successful') ? 'alert-success' : 'alert-error'}`}>
                  {message}
                </div>
              )}

              <button
                type="submit"
                className="btn btn-primary w-full"
                disabled={loading}
              >
                {loading ? (
                  <div className="flex items-center justify-center">
                    <div className="spinner mr-2"></div>
                    Submitting Request...
                  </div>
                ) : (
                  'Submit Request'
                )}
              </button>
            </form>
          </div>
        </div>

        {/* Available Rooms */}
        <div className="lg:col-span-1">
          <div className="card">
            <h3 className="text-lg font-semibold mb-4">Available Rooms</h3>
            <div className="space-y-4">
              {rooms.map(room => (
                <div 
                  key={room.id} 
                  className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                    Number(pref1) === room.id 
                      ? 'border-primary bg-blue-50' 
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                  onClick={() => {
                    setPref1(String(room.id))
                    setRoomConflicts([])
                    loadRoomConflicts(room.id)
                  }}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="font-medium text-gray-900">{room.name}</h4>
                      <p className="text-sm text-gray-600">{room.location}</p>
                    </div>
                    <span className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded">
                      {room.capacity} seats
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {roomConflicts.length > 0 && (
            <div className="card mt-6">
              <h3 className="text-lg font-semibold mb-4">Existing Reservations for Selected Room</h3>
              <div className="space-y-2 text-sm text-gray-700">
                {roomConflicts.map(rc => (
                  <div key={rc.id} className="flex items-center justify-between">
                    <div>
                      <div className="font-medium">{rc.title}</div>
                      <div className="text-xs text-gray-500">
                        {rc.start && new Date(rc.start).toLocaleString()} – {rc.end && new Date(rc.end).toLocaleString()}
                      </div>
                    </div>
                    <span className={`text-xs px-2 py-1 rounded-full ${rc.status === 'CONFIRMED' ? 'bg-green-100 text-green-800' : 'bg-blue-100 text-blue-800'}`}>
                      {rc.status}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Booking Guidelines */}
          <div className="card mt-6">
            <h3 className="text-lg font-semibold mb-4">Booking Guidelines</h3>
            <div className="text-sm text-gray-600 space-y-2">
              <div className="flex items-start">
                <span className="mr-2">⏰</span>
                <span>Minimum 5 days advance notice required before event date</span>
              </div>
              <div className="flex items-start">
                <span className="mr-2">🕐</span>
                <span>Select three room preferences ranked by priority</span>
              </div>
              <div className="flex items-start">
                <span className="mr-2">📅</span>
                <span>Official confirmation will be sent 2 days before the event</span>
              </div>
              <div className="flex items-start">
                <span className="mr-2">⚠️</span>
                <span>Admin approval is required for all room booking requests</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
