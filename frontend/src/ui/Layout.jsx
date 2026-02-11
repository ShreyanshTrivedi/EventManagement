import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'

export default function Layout({ children }) {
  const { user, logout, hasRole } = useAuth()
  const location = useLocation()
  
  const isActive = (path) => location.pathname === path
  
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
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
              <Link 
                to="/" 
                className={`nav-link ${isActive('/') ? 'active' : ''}`}
              >
                Home
              </Link>
              {user && (
                <Link 
                  to="/dashboard" 
                  className={`nav-link ${isActive('/dashboard') ? 'active' : ''}`}
                >
                  Dashboard
                </Link>
              )}
              <Link 
                to="/events" 
                className={`nav-link ${isActive('/events') ? 'active' : ''}`}
              >
                Events
              </Link>
              {user && (hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('ADMIN')) && (
                <>
                  <Link 
                    to="/bookings" 
                    className={`nav-link ${isActive('/bookings') ? 'active' : ''}`}
                  >
                    Bookings
                  </Link>
                  <Link 
                    to="/book-room" 
                    className={`nav-link ${isActive('/book-room') ? 'active' : ''}`}
                  >
                    Book Room
                  </Link>
                </>
              )}
              {user && hasRole('ADMIN') && (
                <>
                  <Link 
                    to="/admin/role-requests" 
                    className={`nav-link ${isActive('/admin/role-requests') ? 'active' : ''}`}
                  >
                    Admin
                  </Link>
                  <Link 
                    to="/admin/room-approvals" 
                    className={`nav-link ${isActive('/admin/room-approvals') ? 'active' : ''}`}
                  >
                    Room Approvals
                  </Link>
                </>
              )}
            </nav>
            
            {/* User Menu */}
            <div className="flex items-center space-x-4">
              {user ? (
                <div className="flex items-center space-x-3">
                  <span className="text-sm text-gray-700">Welcome, {user.sub}</span>
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


