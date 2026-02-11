import React, { useState, useEffect, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../lib/api'

export default function EventRegistration() {
  const { eventId } = useParams()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [fullName, setFullName] = useState('')
  const [answers, setAnswers] = useState({})
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [event, setEvent] = useState(null)
  const [closed, setClosed] = useState(false)

  useEffect(() => {
    api.get(`/api/public/events/${eventId}`)
      .then(res => setEvent(res.data))
      .catch(() => setEvent(null))
  }, [eventId])

  const fields = useMemo(() => {
    if (!event) return []
    try {
      const arr = event.registrationSchema ? JSON.parse(event.registrationSchema) : []
      return Array.isArray(arr) ? arr : []
    } catch { return [] }
  }, [event])

  useEffect(() => {
    if (event && event.startTime) {
      const start = new Date(event.startTime)
      const deadline = new Date(start.getTime() - 2 * 24 * 60 * 60 * 1000)
      setClosed(new Date() > deadline)
    }
  }, [event])

  const onSubmit = async (e) => {
    e.preventDefault()
    setMessage('')

    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}$/
    if (!emailPattern.test(email.trim())) {
      setMessage('Please enter a valid email address.')
      return
    }

    setLoading(true)
    
    try {
      const body = { eventId: Number(eventId), email, fullName, answers }
      await api.post('/api/registrations', body)
      setMessage('Registration successful! You will receive a confirmation email shortly.')
      setTimeout(() => {
        navigate('/events')
      }, 2000)
    } catch (err) {
      setMessage('Registration failed: ' + (err.response?.data || err.message))
    } finally {
      setLoading(false)
    }
  }

  if (!event) {
    return (
      <div className="text-center py-12">
        <div className="spinner mx-auto mb-4"></div>
        <p className="text-gray-600">Loading event details...</p>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-8">
        <button 
          onClick={() => navigate('/events')}
          className="btn btn-secondary btn-sm mb-4"
        >
          ← Back to Events
        </button>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Event Registration</h1>
        <p className="text-gray-600">Complete your registration for this event</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Event Details */}
        <div className="card">
          <h2 className="text-xl font-semibold mb-4">Event Details</h2>
          <div className="space-y-4">
            <div>
              <h3 className="font-medium text-gray-900">{event.title}</h3>
              {event.description && (
                <p className="text-gray-600 text-sm mt-1">{event.description}</p>
              )}
            </div>
            
            <div className="space-y-2">
              <div className="flex items-center text-sm text-gray-500">
                <span className="mr-2">📅</span>
                <span>{new Date(event.startTime).toLocaleDateString()}</span>
              </div>
              <div className="flex items-center text-sm text-gray-500">
                <span className="mr-2">🕐</span>
                <span>
                  {new Date(event.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})} - 
                  {new Date(event.endTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                </span>
              </div>
              <div className="flex items-center text-sm text-gray-500">
                <span className="mr-2">📍</span>
                <span>{event.location || 'TBD'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Registration Form */}
        <div className="card">
          <h2 className="text-xl font-semibold mb-6">Registration Form</h2>

          {closed && (
            <div className="alert alert-error mb-4">
              Registration is closed. Registrations are allowed only until 2 days before the event start.
            </div>
          )}
          
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="form-group">
              <label className="form-label">Full Name</label>
              <input
                type="text"
                className="form-input"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Enter your full name"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input
                type="email"
                className="form-input"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Enter your email address"
                pattern="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}$"
                title="Please enter a valid email address (example@domain.com)"
                required
              />
            </div>

            {/* Dynamic Answers */}
            {fields.filter(f => f !== 'full_name' && f !== 'email').map(key => (
              <div className="form-group" key={key}>
                <label className="form-label">{key.replace(/_/g,' ').replace(/\b\w/g, c => c.toUpperCase())}</label>
                <input className="form-input" value={answers[key] || ''} onChange={e=>setAnswers(prev=>({...prev,[key]:e.target.value}))} />
              </div>
            ))}

            {message && (
              <div className={`alert ${message.includes('successful') ? 'alert-success' : 'alert-error'}`}>
                {message}
              </div>
            )}

            <button
              type="submit"
              className="btn btn-primary w-full"
              disabled={loading || closed}
            >
              {loading ? (
                <div className="flex items-center justify-center">
                  <div className="spinner mr-2"></div>
                  Registering...
                </div>
              ) : (
                'Register for Event'
              )}
            </button>
          </form>

          <div className="mt-6 p-4 bg-gray-50 rounded-lg">
            <h3 className="font-medium text-gray-900 mb-2">Registration Terms</h3>
            <div className="text-sm text-gray-600 space-y-1">
              <div>• Registration is free and open to all campus members</div>
              <div>• You will receive a confirmation email after registration</div>
              <div>• Please arrive 10 minutes before the event starts</div>
              <div>• Contact the organizer if you need to cancel</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
