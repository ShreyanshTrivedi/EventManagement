import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api from '../lib/api'

export default function EventEdit() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [loc, setLoc] = useState('')
  const [club, setClub] = useState('')
  const [selected, setSelected] = useState(['full_name', 'email'])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    api.get(`/api/public/events/${id}`)
      .then(res => {
        const ev = res.data
        setTitle(ev.title || '')
        setDescription(ev.description || '')
        setLoc(ev.location || '')
        setClub(ev.clubId || '')
        if (ev.startTime) setStart(ev.startTime.slice(0,16))
        if (ev.endTime) setEnd(ev.endTime.slice(0,16))
        try {
          const schema = ev.registrationSchema ? JSON.parse(ev.registrationSchema) : []
          if (Array.isArray(schema) && schema.length > 0) {
            setSelected(schema)
          }
        } catch {
          // ignore parse errors
        }
      })
      .catch(() => setError('Failed to load event'))
      .finally(() => setLoading(false))
  }, [id])

  const toggleField = (key) => {
    setSelected(prev => prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key])
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setMessage('')
    setSaving(true)
    try {
      if (!start || !end) {
        setError('Please provide start and end date/time values')
        return
      }
      // datetime-local presents local time as 'YYYY-MM-DDTHH:MM'. Append seconds and send as-is
      const startValue = `${start}:00`
      const endValue = `${end}:00`
      const registrationSchema = JSON.stringify(selected)
      const res = await api.put(`/api/events/${id}`, {
        title: title.trim(),
        description: description.trim(),
        start: startValue,
        end: endValue,
        location: loc.trim() || undefined,
        clubId: club || undefined,
        registrationSchema
      })
      if (res.status === 200) {
        setMessage('Event updated')
        setTimeout(() => navigate('/dashboard'), 800)
      } else {
        setError('Failed to update event')
      }
    } catch (err) {
      setError(err.response?.data || 'Failed to update event')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="text-center py-12">
        <div className="spinner mx-auto mb-4"></div>
        <p className="text-gray-600">Loading event...</p>
      </div>
    )
  }

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Edit Event</h1>
          <p className="text-slate-600">Update event details. Editing is blocked within 2 days of the start time.</p>
        </div>
        <button type="button" className="btn btn-secondary" onClick={() => navigate('/dashboard')}>Back</button>
      </div>

      <div className="card">
        <form onSubmit={onSubmit} className="space-y-6">
          <div className="rounded-xl border border-slate-200 bg-white/70 p-4">
            <div className="text-sm font-semibold text-slate-900">Event details</div>
            <div className="mt-1 text-sm text-slate-600">Update title, location and schedule.</div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="form-group">
              <label className="form-label">Event Title</label>
              <input className="form-input" value={title} onChange={e => setTitle(e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">Location (optional)</label>
              <input className="form-input" value={loc} onChange={e => setLoc(e.target.value)} />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Description</label>
            <textarea className="form-input" rows={4} value={description} onChange={e => setDescription(e.target.value)} />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="form-group">
              <label className="form-label">Start</label>
              <input type="datetime-local" className="form-input" value={start} onChange={e => setStart(e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">End</label>
              <input type="datetime-local" className="form-input" value={end} onChange={e => setEnd(e.target.value)} required />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Club ID (optional)</label>
            <input className="form-input" value={club} onChange={e => setClub(e.target.value)} />
          </div>
          </div>

          <div className="rounded-xl border border-slate-200 bg-white/70 p-4">
            <div className="text-sm font-semibold text-slate-900">Registration fields</div>
            <div className="mt-1 text-sm text-slate-600">Choose what attendees must fill during registration.</div>
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
              {['full_name','email','phone','student_id','department','year','tshirt_size','dietary_pref','comments'].map(key => (
                <label key={key} className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white/70 px-3 py-2 text-sm text-slate-700">
                  <input type="checkbox" checked={selected.includes(key)} onChange={() => toggleField(key)} />
                  <span>{key.replace(/_/g,' ').replace(/\b\w/g,c=>c.toUpperCase())}</span>
                </label>
              ))}
            </div>
          </div>

          {error && <div className="alert alert-error">{error}</div>}
          {message && <div className="alert alert-success">{message}</div>}

          <div className="flex flex-col sm:flex-row gap-3 sm:justify-end">
            <button type="button" className="btn btn-secondary" onClick={() => navigate('/dashboard')} disabled={saving}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
