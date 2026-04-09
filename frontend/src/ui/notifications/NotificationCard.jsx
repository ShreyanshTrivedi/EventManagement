import React from 'react'
import { formatRelativeTime } from '../design-system'

export default function NotificationCard({ item, onOpen, onMarkRead, onReply, onMute }) {
  const urgencyClass = item.urgency === 'HIGH' ? 'bg-red-50 text-red-700' : item.urgency === 'LOW' ? 'bg-emerald-50 text-emerald-700' : 'bg-blue-50 text-blue-700'
  return (
    <div className="card card-compact flex items-start gap-4 fade-in-up interactive" role="article" aria-label={`Notification ${item.title}`}>
      <div className={`w-12 h-12 rounded-full flex items-center justify-center ${urgencyClass} font-semibold text-sm`}>{item.urgency === 'HIGH' ? '!' : item.urgency === 'LOW' ? '✓' : '·'}</div>
      <div className="flex-1">
        <div className="flex items-start justify-between">
          <div>
            <div className="font-semibold text-slate-900">{item.title}</div>
            <div className="text-xs text-gray-500 mt-1">{formatRelativeTime(item.createdAt)}</div>
          </div>
          <div className="text-sm text-gray-400">{item.event ? 'Event' : 'Global'}</div>
        </div>
        <div className="mt-2 text-sm text-gray-600 truncate" style={{ maxWidth: 520 }}>{item.message}</div>
        <div className="mt-3 flex items-center gap-3">
          {item.threadEnabled && <button onClick={() => onReply && onReply(item)} className="btn btn-ghost btn-sm" aria-label="Reply">Reply</button>}
          <button onClick={() => onMarkRead && onMarkRead(item)} className="text-sm text-gray-500 hover:text-gray-700" aria-label={item.read ? 'Mark as unread' : 'Mark as read'}>{item.read ? 'Read' : 'Mark read'}</button>
          <button onClick={() => onMute && onMute(item)} className="text-sm text-gray-500 hover:text-gray-700" aria-label={item.muted ? 'Unmute' : 'Mute'}>{item.muted ? 'Unmute' : 'Mute'}</button>
        </div>
      </div>
    </div>
  )
}
