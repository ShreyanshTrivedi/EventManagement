import React, { useState, useEffect } from 'react'
import api from '../lib/api'

const RoomScheduleCalendar = ({ roomId, view = 'week' }) => {
  const [schedule, setSchedule] = useState([])
  const [selectedDate, setSelectedDate] = useState(new Date())
  const [loading, setLoading] = useState(true)
  const [roomDetails, setRoomDetails] = useState(null)
  
  const timeSlots = [
    '9:00-9:50', '9:50-10:40', '10:40-11:30', '11:30-12:20',
    '12:20-13:10', '13:10-14:00', '14:00-14:50', '14:50-15:40',
    '15:40-16:30', '16:30-17:20', '17:20-18:10'
  ]
  
  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
  
  useEffect(() => {
    if (roomId) {
      fetchRoomDetails()
      fetchSchedule()
    }
  }, [roomId, selectedDate, view])
  
  const fetchRoomDetails = async () => {
    try {
      const response = await api.get(`/api/room-management/rooms/${roomId}`)
      setRoomDetails(response.data)
    } catch (error) {
      console.error('Failed to fetch room details:', error)
    }
  }
  
  const fetchSchedule = async () => {
    try {
      setLoading(true)
      let endpoint
      
      if (view === 'week') {
        endpoint = `/api/timetable/room/${roomId}/week`
      } else {
        // For day view, get schedule for each day of the week
        const weekSchedule = []
        const currentDate = new Date(selectedDate)
        const startOfWeek = new Date(currentDate)
        startOfWeek.setDate(currentDate.getDate() - currentDate.getDay() + 1) // Start from Monday
        
        for (let i = 0; i < 7; i++) {
          const date = new Date(startOfWeek)
          date.setDate(startOfWeek.getDate() + i)
          
          try {
            const response = await api.get(`/api/timetable/room/${roomId}/day/${date.toISOString().split('T')[0]}`)
            weekSchedule.push({
              dayOfWeek: days[i],
              date: date.toISOString().split('T')[0],
              schedule: response.data.combinedSchedule || []
            })
          } catch (error) {
            weekSchedule.push({
              dayOfWeek: days[i],
              date: date.toISOString().split('T')[0],
              schedule: []
            })
          }
        }
        
        setSchedule(weekSchedule)
        setLoading(false)
        return
      }
      
      const response = await api.get(endpoint)
      setSchedule(response.data)
    } catch (error) {
      console.error('Failed to fetch schedule:', error)
      setSchedule([])
    } finally {
      setLoading(false)
    }
  }
  
  const getScheduleItem = (day, timeSlot) => {
    const [startTime] = timeSlot.split('-')
    
    if (view === 'week') {
      // For week view, schedule is an array of FixedTimetable objects
      const item = schedule.find(s => 
        s.dayOfWeek === day.toUpperCase() && 
        s.startTime === startTime + ':00'
      )
      
      if (!item) return null
      
      return (
        <div className="p-2 rounded text-xs bg-blue-100 border border-blue-300">
          <div className="font-semibold">{item.courseName}</div>
          <div className="text-gray-600">{item.courseCode} - {item.section}</div>
          <div className="text-gray-500">{item.facultyName || ''}</div>
        </div>
      )
    } else {
      // For day view, schedule is grouped by day
      const daySchedule = schedule.find(s => s.dayOfWeek === day)
      if (!daySchedule) return null
      
      const item = daySchedule.schedule.find(s => s.startTime === startTime + ':00')
      
      if (!item) return null
      
      return (
        <div className={`p-2 rounded text-xs ${
          item.type === 'FIXED_CLASS' 
            ? 'bg-blue-100 border-blue-300' 
            : 'bg-green-100 border-green-300'
        } border`}>
          <div className="font-semibold">{item.title}</div>
          <div className="text-gray-600">{item.subtitle}</div>
          <div className="text-gray-500">{item.facultyName}</div>
        </div>
      )
    }
  }
  
  const navigateWeek = (direction) => {
    const newDate = new Date(selectedDate)
    newDate.setDate(selectedDate.getDate() + (direction * 7))
    setSelectedDate(newDate)
  }
  
  const getWeekRange = () => {
    const startOfWeek = new Date(selectedDate)
    startOfWeek.setDate(selectedDate.getDate() - selectedDate.getDay() + 1)
    
    const endOfWeek = new Date(startOfWeek)
    endOfWeek.setDate(startOfWeek.getDate() + 6)
    
    return `${startOfWeek.toLocaleDateString()} - ${endOfWeek.toLocaleDateString()}`
  }
  
  if (loading) return <div className="text-center py-8">Loading schedule...</div>
  
  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <div className="p-4 border-b bg-gray-50">
        <div className="flex justify-between items-center">
          <div>
            <h3 className="text-lg font-semibold">Room Schedule</h3>
            {roomDetails && (
              <p className="text-sm text-gray-600">
                {roomDetails.roomNumber} - {roomDetails.name} (Capacity: {roomDetails.capacity})
              </p>
            )}
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => navigateWeek(-1)}
              className="px-3 py-1 border rounded hover:bg-gray-50 text-sm"
            >
              Previous
            </button>
            <span className="px-3 py-1 text-sm font-medium">
              {view === 'week' ? getWeekRange() : selectedDate.toLocaleDateString()}
            </span>
            <button
              onClick={() => navigateWeek(1)}
              className="px-3 py-1 border rounded hover:bg-gray-50 text-sm"
            >
              Next
            </button>
          </div>
        </div>
      </div>
      
      <div className="overflow-x-auto">
        <table className="w-full border-collapse">
          <thead>
            <tr>
              <th className="border p-2 bg-gray-50 text-left font-medium">Time</th>
              {days.map(day => (
                <th key={day} className="border p-2 bg-gray-50 text-center font-medium min-w-[120px]">
                  <div>{day}</div>
                  {view === 'day' && schedule.find(s => s.dayOfWeek === day) && (
                    <div className="text-xs text-gray-500 font-normal">
                      {new Date(schedule.find(s => s.dayOfWeek === day).date).toLocaleDateString()}
                    </div>
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {timeSlots.map(timeSlot => (
              <tr key={timeSlot} className="hover:bg-gray-50">
                <td className="border p-2 bg-gray-50 font-medium text-sm">
                  {timeSlot}
                </td>
                {days.map(day => (
                  <td key={`${day}-${timeSlot}`} className="border p-1 h-[60px] align-top">
                    {getScheduleItem(day, timeSlot)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      
      {/* Legend */}
      <div className="p-4 border-t bg-gray-50">
        <div className="flex gap-6 text-sm">
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 bg-blue-100 border border-blue-300 rounded"></div>
            <span>Fixed Class</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 bg-green-100 border border-green-300 rounded"></div>
            <span>Booking</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 border border-gray-300 rounded"></div>
            <span>Available</span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default RoomScheduleCalendar
