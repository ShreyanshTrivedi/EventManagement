import React, { useState } from 'react'
import api from '../lib/api'
import { useAuth } from '../lib/AuthContext'
import { useNavigate, Link } from 'react-router-dom'
import { jwtDecode } from 'jwt-decode'
import { showToast } from '../lib/toast'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const { setToken } = useAuth()
  const navigate = useNavigate()

  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await api.post('/api/auth/login', { username: username.trim(), password })
      if (res.status === 200 && res.data && res.data.token) {
        setToken(res.data.token)
        showToast({ message: 'Signed in', type: 'success' })
        // Role-based redirect
        let to = '/dashboard'
        try {
          const decoded = jwtDecode(res.data.token)
          const roles = Array.isArray(decoded.roles) ? decoded.roles.map(r => String(r).replace(/^ROLE_/, '')) : []
          if (roles.includes('GENERAL_USER')) to = '/'
          else if (roles.includes('FACULTY') || roles.includes('CLUB_ASSOCIATE') || roles.includes('ADMIN')) to = '/dashboard'
        } catch {}
        navigate(to)
      } else if (res.status === 401) {
        setError('Invalid password. Please try again.')
        showToast({ message: 'Invalid username or password', type: 'error' })
      } else if (res.status === 404) {
        setError('User not found.')
        showToast({ message: 'User not found', type: 'error' })
      } else {
        setError('Login failed. Please try again.')
        showToast({ message: 'Login failed', type: 'error' })
      }
    } catch (err) {
      if (err.response) {
        if (err.response.status === 401) setError('Invalid password. Please try again.')
        else if (err.response.status === 404) setError('User not found.')
        else setError('Login failed. Please try again.')
      } else {
        setError('Network error. Please check your connection.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-md mx-auto">
      <div className="card p-0 overflow-hidden">
        <div className="px-6 py-5 border-b border-slate-200 bg-gradient-to-br from-blue-50 to-white">
          <div className="text-sm font-semibold text-blue-700">EventSphere</div>
          <div className="mt-1 text-2xl font-bold text-slate-900">Welcome back</div>
          <div className="text-sm text-slate-600">Sign in to continue</div>
        </div>

        <div className="p-6">
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="form-group">
              <label className="form-label">Username</label>
              <input
                type="text"
                className="form-input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter your username"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  className="form-input pr-10"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  required
                  aria-invalid={!!error}
                />
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowPassword(v => !v)} style={{ position: 'absolute', right: '0.5rem', top: '0.45rem' }} aria-label={showPassword ? 'Hide password' : 'Show password'}>
                  {showPassword ? '🙈' : '👁️'}
                </button>
              </div>
            </div>

            {error && (
              <div className="alert alert-error">
                {error}
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
                  Signing in...
                </div>
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-slate-600">
              New here? <Link to="/register" className="text-blue-700 hover:underline">Create an account</Link>
            </p>
          </div>

          <div className="mt-6">
            <div className="rounded-xl border border-slate-200 bg-white/60 p-4">
              <div className="text-xs font-semibold text-slate-700">Demo accounts</div>
              <div className="mt-2 grid grid-cols-1 gap-1 text-xs text-slate-600">
                <div><span className="font-medium text-slate-700">admin</span> / Admin@123</div>
                <div><span className="font-medium text-slate-700">faculty</span> / Faculty@123</div>
                <div><span className="font-medium text-slate-700">club</span> / Club@123</div>
                <div><span className="font-medium text-slate-700">user</span> / User@123</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}