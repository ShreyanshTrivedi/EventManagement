import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../lib/api'
import { showToast } from '../lib/toast'

export default function Register() {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [role, setRole] = useState('GENERAL_USER')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  const navigate = useNavigate()

  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setMessage('')
    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}$/
    if (!emailPattern.test(email.trim())) {
      setError('Please enter a valid email address.')
      showToast({ message: 'Please enter a valid email address', type: 'error' })
      return
    }
    if (password !== confirm) {
      setError('Passwords do not match.')
      showToast({ message: 'Passwords do not match', type: 'error' })
      return
    }
    try {
      setLoading(true)
      const res = await api.post('/api/auth/register', { username: username.trim(), email: email.trim(), password, role })
      if (res.status === 200) {
        const msg = typeof res.data === 'string' ? res.data : 'Registration successful. You can now sign in.'
        setMessage(msg)
        showToast({ message: 'Registration successful', type: 'success' })
        if (role === 'GENERAL_USER') {
          setTimeout(() => navigate('/login'), 800)
        }
      } else {
        setError('Registration failed. Please try again.')
        showToast({ message: 'Registration failed', type: 'error' })
      }
    } catch (err) {
      if (err.response && err.response.data) setError(String(err.response.data))
      else setError('Network error. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-md mx-auto">
      <div className="card p-0 overflow-hidden">
        <div className="px-6 py-5 border-b border-slate-200 bg-gradient-to-br from-blue-50 to-white">
          <div className="text-sm font-semibold text-blue-700">EventSphere</div>
          <div className="mt-1 text-2xl font-bold text-slate-900">Create account</div>
          <div className="text-sm text-slate-600">General users are registered instantly. Other roles need admin approval.</div>
        </div>

        <div className="p-6">
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="form-group">
              <label className="form-label">Username</label>
              <input className="form-input" value={username} onChange={(e)=>setUsername(e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">Role</label>
              <select className="form-select" value={role} onChange={(e)=>setRole(e.target.value)}>
                <option value="GENERAL_USER">General User</option>
                <option value="FACULTY">Faculty</option>
                <option value="CLUB_ASSOCIATE">Club Associate</option>
                <option value="ADMIN">Admin</option>
              </select>
              <div className="mt-2 text-xs text-slate-500">
                General users can register for events. Faculty/Club/Admin roles require approval.
              </div>
            </div>
          <div className="form-group">
            <label className="form-label">Email</label>
            <input
              type="email"
              className="form-input"
              value={email}
              onChange={(e)=>setEmail(e.target.value)}
              pattern="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}$"
              title="Please enter a valid email address (example@domain.com)"
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">Password</label>
            <input type="password" className="form-input" value={password} onChange={(e)=>setPassword(e.target.value)} required />
          </div>
          <div className="form-group">
            <label className="form-label">Confirm Password</label>
            <input type="password" className="form-input" value={confirm} onChange={(e)=>setConfirm(e.target.value)} required />
          </div>

            {error && <div className="alert alert-error" role="alert" aria-live="assertive">{error}</div>}
            {message && <div className="alert alert-success" role="status" aria-live="polite">{message}</div>}

            <button type="submit" className="btn btn-primary w-full" disabled={loading}>
              {loading ? 'Creating account...' : 'Create Account'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-slate-600">Already have an account? <Link to="/login" className="text-blue-700 hover:underline">Sign in</Link></p>
          </div>
        </div>
      </div>
    </div>
  )
}
