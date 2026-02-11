import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import api from '../lib/api'

export default function Events() {
  const [events, setEvents] = useState([])
  const [eventList, setEventList] = useState([])
  const [loading, setLoading] = useState(true)
  const { hasRole, clubId } = useAuth()

  useEffect(() => {
    api.get('/api/public/events').then((res) => {
      const mapped = (res.data || []).map(e => ({ 
        id: e.id, 
        title: e.title, 
        start: e.startTime, 
        end: e.endTime,
        backgroundColor: '#3b82f6',
        borderColor: '#2563eb'
      }))
      setEvents(mapped)
      setEventList(res.data || [])
    }).catch(() => {
      setEvents([])
      setEventList([])
    }).finally(() => {
      setLoading(false)
    })
  }, [])

  if (loading) {
    return (
      <div className="text-center py-12">
        <div className="spinner mx-auto mb-4"></div>
        <p className="text-gray-600">Loading events...</p>
      </div>
    )
  }

  const canRegister = (evt) => {
    if (hasRole('ADMIN') || hasRole('FACULTY')) return false
    if (hasRole('CLUB_ASSOCIATE')) {
      if (clubId && evt.clubId && evt.clubId === clubId) return false
    }
    return true
  }

  const now = new Date()
  const in15Days = new Date(now.getTime() + 15 * 24 * 60 * 60 * 1000)
  const upcomingEvents = (eventList || [])
    .filter(event => {
      if (!event.startTime) return false
      const start = new Date(event.startTime)
      return start >= now && start <= in15Days
    })
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Campus Events</h1>
        <p className="text-gray-600">Discover and register for upcoming events</p>
        {(hasRole('ADMIN') || hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE')) && (
          <div className="mt-4">
            <Link to="/events/create" className="btn btn-primary">Create Event</Link>
          </div>
        )}
      </div>

      {/* Calendar View */}
      <div className="card mb-8">
        <h2 className="text-xl font-semibold mb-4">Event Calendar</h2>
        <FullCalendar 
          plugins={[dayGridPlugin, timeGridPlugin]} 
          initialView="dayGridMonth" 
          events={events} 
          height="auto"
          headerToolbar={{
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek'
          }}
          eventClick={(info) => {
            const event = eventList.find(e => e.id === info.event.id)
            if (event && canRegister(event)) {
              window.location.href = `/register/${event.id}`
            }
          }}
        />
      </div>
      
      {/* Event List */}
      <div className="card">
        <h2 className="text-xl font-semibold mb-6">Upcoming Events</h2>
        {upcomingEvents.length === 0 ? (
          <div className="text-center py-8">
            <div className="text-4xl mb-4">📅</div>
            <p className="text-gray-600">No upcoming events in the next 15 days</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {upcomingEvents.map(event => (
              <div key={event.id} className="border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between mb-3">
                  <h3 className="text-lg font-semibold text-gray-900">{event.title}</h3>
                  <span className="bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded-full">
                    Public
                  </span>
                </div>
                
                {event.description && (
                  <p className="text-gray-600 mb-4 text-sm">{event.description}</p>
                )}
                
                <div className="space-y-2 mb-4">
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
                    <span>Location: {event.location || 'TBD'}</span>
                  </div>
                </div>
                
                {canRegister(event) ? (
                  <Link 
                    to={`/register/${event.id}`} 
                    className="btn btn-primary btn-sm w-full"
                  >
                    Register for Event
                  </Link>
                ) : (
                  <button className="btn btn-secondary btn-sm w-full" disabled>
                    Registration not available
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}


