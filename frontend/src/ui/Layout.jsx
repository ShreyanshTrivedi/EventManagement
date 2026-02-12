import React, { useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'

export default function Layout({ children }) {
  const { user, logout, hasRole } = useAuth()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  
  const isActive = (path) => location.pathname === path

  const links = useMemo(() => {
    const items = [
      { to: '/', label: 'Home', show: true },
      { to: '/dashboard', label: 'Dashboard', show: !!user },
      { to: '/events', label: 'Events', show: true },
      { to: '/bookings', label: 'Bookings', show: !!user && (hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('ADMIN')) },
      { to: '/enhanced-book-room', label: 'Book Room', show: !!user && (hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('ADMIN')) },
      { to: '/admin/role-requests', label: 'Admin', show: !!user && hasRole('ADMIN') },
      { to: '/admin/room-approvals', label: 'Room Approvals', show: !!user && hasRole('ADMIN') },
    ]
    return items.filter(i => i.show)
  }, [user, hasRole])
  
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white/80 backdrop-blur supports-[backdrop-filter]:bg-white/70 shadow-sm border-b border-gray-200 sticky top-0">
        <div className="container">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <div className="flex items-center">
              <Link to="/" className="logo-link">
                🎓 Campus Events
              </Link>
            </div>
            
            {/* Navigation */}
            <nav className="hidden md:flex items-center">
              {links.map(l => (
                <Link
                  key={l.to}
                  to={l.to}
                  className={`nav-link ${isActive(l.to) ? 'active' : ''}`}
                >
                  {l.label}
                </Link>
              ))}
            </nav>
            
            {/* User Menu */}
            <div className="flex items-center space-x-4">
              <button
                type="button"
                className="md:hidden btn btn-secondary btn-sm"
                onClick={() => setMobileOpen(v => !v)}
              >
                Menu
              </button>
              {user ? (
                <div className="flex items-center space-x-3">
                  <span className="hidden sm:inline text-sm text-gray-700">Welcome, {user.sub}</span>
                  <button 
                    onClick={logout}
                    className="btn btn-secondary btn-sm"
                  >
                    Logout
                  </button>
                </div>
              ) : (
                <div className="flex items-center space-x-2">
                  <Link to="/login" className="btn btn-primary btn-sm">Login</Link>
                  <Link to="/register" className="btn btn-secondary btn-sm">Register</Link>
                </div>
              )}
            </div>
          </div>
        </div>

        {mobileOpen && (
          <div className="md:hidden border-t border-gray-200">
            <div className="container py-2">
              <nav className="flex flex-col">
                {links.map(l => (
                  <Link
                    key={l.to}
                    to={l.to}
                    className={`nav-link ${isActive(l.to) ? 'active' : ''}`}
                    onClick={() => setMobileOpen(false)}
                  >
                    {l.label}
                  </Link>
                ))}
              </nav>
            </div>
          </div>
        )}
        
      </header>
      
      {/* Main Content */}
      <main className="container py-8">
        {children}
      </main>
      
      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 mt-12">
        <div className="container py-8">
          <div className="text-center text-gray-500">
            <p>&copy; 2024 Campus Events. Built with React & Spring Boot.</p>
          </div>
        </div>
      </footer>
    </div>
  )
}


