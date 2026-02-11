import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../lib/api'

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
      return
    }
    if (password !== confirm) {
      setError('Passwords do not match.')
      return
    }
    try {
      setLoading(true)
      const res = await api.post('/api/auth/register', { username: username.trim(), email: email.trim(), password, role })
      if (res.status === 200) {
        const msg = typeof res.data === 'string' ? res.data : 'Registration successful. You can now sign in.'
        setMessage(msg)
        if (role === 'GENERAL_USER') {
          setTimeout(() => navigate('/login'), 800)
        }
      } else {
        setError('Registration failed. Please try again.')
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
      <div className="card">
        <div className="text-center mb-6">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">Create Account</h1>
          <p className="text-gray-600">Choose a role. General Users are registered instantly. Other roles require admin approval.</p>
        </div>

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

          {error && <div className="alert alert-error">{error}</div>}
          {message && <div className="alert alert-success">{message}</div>}

          <button type="submit" className="btn btn-primary w-full" disabled={loading}>
            {loading ? 'Creating account...' : 'Create Account'}
          </button>
        </form>

        <div className="mt-6 text-center">
          <p className="text-sm text-gray-600">Already have an account? <Link to="/login" className="text-primary hover:underline">Sign in</Link></p>
        </div>
      </div>
    </div>
  )
}
