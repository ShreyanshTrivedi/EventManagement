import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { jwtDecode } from 'jwt-decode'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'))
  const [user, setUser] = useState(null)
  const [roles, setRoles] = useState([])
  const [clubId, setClubId] = useState(null)

  useEffect(() => {
    if (token) {
      localStorage.setItem('token', token)
      try {
        const decoded = jwtDecode(token)
        setUser(decoded)
        const r = Array.isArray(decoded.roles) ? decoded.roles : []
        setRoles(r.map(x => String(x).replace(/^ROLE_/, '')))
        setClubId(decoded.clubId || null)
      } catch {
        setUser(null)
        setRoles([])
        setClubId(null)
      }
    } else {
      localStorage.removeItem('token')
      setUser(null)
      setRoles([])
      setClubId(null)
    }
  }, [token])

  const value = useMemo(() => ({
    token,
    setToken,
    user,
    roles,
    clubId,
    hasRole: (r) => roles.includes(r) || roles.includes(String(r).toUpperCase()),
    logout: () => setToken(null)
  }), [token, user, roles, clubId])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() { return useContext(AuthContext) }


