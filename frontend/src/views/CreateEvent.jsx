import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import api from '../lib/api'

const FIELD_OPTIONS = [
  { key: 'full_name', label: 'Full Name' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Phone Number' },
  { key: 'student_id', label: 'Student ID' },
  { key: 'department', label: 'Department' },
  { key: 'year', label: 'Year' },
  { key: 'tshirt_size', label: 'T-shirt Size' },
  { key: 'dietary_pref', label: 'Dietary Preference' },
  { key: 'comments', label: 'Comments' }
]

export default function CreateEvent() {
  const { hasRole, clubId } = useAuth()
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [loc, setLoc] = useState('')
  const [club, setClub] = useState(clubId || '')
  const [selected, setSelected] = useState(['full_name', 'email'])
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  const allowed = hasRole('ADMIN') || hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE')

  const toggleField = (key) => {
    setSelected((prev) => prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key])
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!allowed) { setError('Not authorized') ; return }
    setError('')
    setMessage('')
    try {
      setLoading(true)
      const registrationSchema = JSON.stringify(selected)
      // datetime-local gives 'YYYY-MM-DDTHH:MM' in local time.
      // Append seconds and send as-is so backend LocalDateTime stores the same local time.
      const startValue = start ? `${start}:00` : null
      const endValue = end ? `${end}:00` : null
      const res = await api.post('/api/events', {
        title: title.trim(),
        description: description.trim(),
        start: startValue,
        end: endValue,
        location: loc.trim() || undefined,
        clubId: club || undefined,
        registrationSchema
      })
      if (res.status === 200) {
        setMessage('Event created')
        setTimeout(() => navigate('/events'), 600)
      } else {
        setError('Failed to create event')
      }
    } catch (err) {
      setError(err.response?.data || 'Failed to create event')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Create Event</h1>
        <p className="text-gray-600">Provide event details and choose registration fields for attendees.</p>
      </div>

      {!allowed ? (
        <div className="alert alert-error">You are not authorized to create events.</div>
      ) : (
        <div className="card">
          <form onSubmit={onSubmit} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="form-group">
                <label className="form-label">Event Title</label>
                <input className="form-input" value={title} onChange={(e)=>setTitle(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Location (optional)</label>
                <input className="form-input" value={loc} onChange={(e)=>setLoc(e.target.value)} placeholder="Defaults to TBD" />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Description</label>
              <textarea className="form-input" value={description} onChange={(e)=>setDescription(e.target.value)} />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="form-group">
                <label className="form-label">Start</label>
                <input type="datetime-local" className="form-input" value={start} onChange={(e)=>setStart(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">End</label>
                <input type="datetime-local" className="form-input" value={end} onChange={(e)=>setEnd(e.target.value)} required />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Club ID (optional)</label>
              <input className="form-input" value={club} onChange={(e)=>setClub(e.target.value)} placeholder="If empty, uses your club automatically" />
            </div>

            <div>
              <label className="form-label">Registration Fields</label>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                {FIELD_OPTIONS.map(opt => (
                  <label key={opt.key} className="inline-flex items-center space-x-2">
                    <input type="checkbox" checked={selected.includes(opt.key)} onChange={()=>toggleField(opt.key)} />
                    <span className="text-sm text-gray-700">{opt.label}</span>
                  </label>
                ))}
              </div>
              <p className="text-xs text-gray-500 mt-2">Full Name and Email are preselected by default.</p>
            </div>

            {error && <div className="alert alert-error">{error}</div>}
            {message && <div className="alert alert-success">{message}</div>}

            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Creating...' : 'Create Event'}
            </button>
          </form>
        </div>
      )}
    </div>
  )
}
