import React from 'react'
import { Link } from 'react-router-dom'

export default function Landing() {
  return (
    <div>
      {/* Hero Section */}
      <div className="mb-12">
        <div className="card overflow-hidden p-0">
          <div className="px-8 py-10 bg-gradient-to-br from-blue-50 via-white to-emerald-50">
            <div className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white/70 px-3 py-1 text-xs font-semibold text-slate-700">
              <span className="text-blue-700">New</span>
              <span className="text-slate-500">Modern campus event & room booking hub</span>
            </div>

            <div className="mt-5 text-4xl md:text-6xl font-bold text-slate-900 leading-tight">
              Run better campus events with
              {' '}
              <span className="text-blue-700">clarity</span>
              {' '}
              and
              {' '}
              <span className="text-emerald-700">confidence</span>.
            </div>

            <p className="mt-4 text-lg text-slate-600 max-w-2xl">
              Discover events, register in seconds, and manage room requests with fewer conflicts and a cleaner workflow.
            </p>

            <div className="mt-6 flex flex-col sm:flex-row gap-3">
              <Link to="/events" className="btn btn-primary btn-lg">
                Browse Events
              </Link>
              <Link to="/book-room" className="btn btn-secondary btn-lg">
                Book a Room
              </Link>
            </div>

            <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="rounded-xl border border-slate-200 bg-white/70 p-4">
                <div className="text-sm font-semibold text-slate-900">Calendar-first discovery</div>
                <div className="mt-1 text-sm text-slate-600">A quick view of what’s happening this month.</div>
              </div>
              <div className="rounded-xl border border-slate-200 bg-white/70 p-4">
                <div className="text-sm font-semibold text-slate-900">Registration schemas</div>
                <div className="mt-1 text-sm text-slate-600">Collect only what you need from attendees.</div>
              </div>
              <div className="rounded-xl border border-slate-200 bg-white/70 p-4">
                <div className="text-sm font-semibold text-slate-900">Room conflict protection</div>
                <div className="mt-1 text-sm text-slate-600">Availability checks that match real booking rules.</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Features Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
        <div className="card">
          <div className="h-12 w-12 rounded-xl flex items-center justify-center" style={{ background: 'rgba(59,130,246,0.12)' }}>
            <div className="text-2xl">📅</div>
          </div>
          <h3 className="text-xl font-semibold mt-4">Event Discovery</h3>
          <p className="text-slate-600 mt-2">
            Browse upcoming events with an interactive calendar. Find workshops, lectures, and social events happening around campus.
          </p>
        </div>
        
        <div className="card">
          <div className="h-12 w-12 rounded-xl flex items-center justify-center" style={{ background: 'rgba(16,185,129,0.12)' }}>
            <div className="text-2xl">🏢</div>
          </div>
          <h3 className="text-xl font-semibold mt-4">Room Booking</h3>
          <p className="text-slate-600 mt-2">
            Book conference rooms, lecture halls, and meeting spaces. Check availability and avoid clashes.
          </p>
        </div>
        
        <div className="card">
          <div className="h-12 w-12 rounded-xl flex items-center justify-center" style={{ background: 'rgba(245,158,11,0.16)' }}>
            <div className="text-2xl">👥</div>
          </div>
          <h3 className="text-xl font-semibold mt-4">Community</h3>
          <p className="text-slate-600 mt-2">
            Engage students, faculty, and staff. Register for events and stay on top of updates.
          </p>
        </div>
      </div>

    </div>
  )
}


