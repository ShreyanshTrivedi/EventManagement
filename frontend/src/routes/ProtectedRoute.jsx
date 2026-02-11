import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'

export default function ProtectedRoute({ children, roles }) {
  const { token, hasRole } = useAuth()
  const location = useLocation()

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (roles && roles.length > 0) {
    const ok = roles.some(r => hasRole(r))
    if (!ok) {
      return <Navigate to="/" replace />
    }
  }

  return children
}
