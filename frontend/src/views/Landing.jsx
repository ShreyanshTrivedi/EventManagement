import React from 'react'
import { Link } from 'react-router-dom'

export default function Landing() {
  return (
    <div>
      {/* Hero Section */}
      <div className="text-center mb-12">
        <h1 className="text-4xl md:text-6xl font-bold text-gray-900 mb-6">
          Welcome to <span className="text-primary">Campus Events</span>
        </h1>
        <p className="text-xl text-gray-600 mb-8 max-w-2xl mx-auto">
          Discover, register, and manage campus events and room bookings all in one place. 
          Connect with your community and make the most of your campus experience.
        </p>
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link to="/events" className="btn btn-primary btn-lg">
            Browse Events
          </Link>
          <Link to="/book-room" className="btn btn-secondary btn-lg">
            Book a Room
          </Link>
        </div>
      </div>

      {/* Features Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
        <div className="card text-center">
          <div className="text-4xl mb-4">📅</div>
          <h3 className="text-xl font-semibold mb-2">Event Discovery</h3>
          <p className="text-gray-600">
            Browse upcoming events with our interactive calendar. Find workshops, lectures, and social events happening around campus.
          </p>
        </div>
        
        <div className="card text-center">
          <div className="text-4xl mb-4">🏢</div>
          <h3 className="text-xl font-semibold mb-2">Room Booking</h3>
          <p className="text-gray-600">
            Easily book conference rooms, lecture halls, and meeting spaces. Check availability and avoid conflicts.
          </p>
        </div>
        
        <div className="card text-center">
          <div className="text-4xl mb-4">👥</div>
          <h3 className="text-xl font-semibold mb-2">Community</h3>
          <p className="text-gray-600">
            Connect with fellow students, faculty, and staff. Register for events and stay updated with notifications.
          </p>
        </div>
      </div>

    </div>
  )
}


