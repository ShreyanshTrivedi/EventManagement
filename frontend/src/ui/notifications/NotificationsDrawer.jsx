import React, { useEffect, useState } from 'react'
import { fetchInbox, markDeliveryRead, muteDelivery, createThread } from '../../lib/api'
import NotificationCard from './NotificationCard'

export default function NotificationsDrawer({ open, onClose }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!open) return
    const onKey = (e) => { if (e.key === 'Escape') onClose && onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])


  useEffect(() => {
    if (!open) return
    let mounted = true
    setLoading(true)
    fetchInbox().then(res => {
      if (!mounted) return
      setItems(res.data || [])
    }).catch(() => {}).finally(() => { if (mounted) setLoading(false) })
    return () => { mounted = false }
  }, [open])

  const handleMarkRead = async (item) => {
    try {
      await markDeliveryRead(item.deliveryId)
      setItems(prev => prev.map(i => i.deliveryId === item.deliveryId ? { ...i, read: true } : i))
    } catch (err) {
      console.error(err)
    }
  }

  const handleMute = async (item) => {
    try {
      await muteDelivery(item.deliveryId, !item.muted)
      setItems(prev => prev.map(i => i.deliveryId === item.deliveryId ? { ...i, muted: !i.muted } : i))
    } catch (err) { console.error(err) }
  }

  const handleReply = async (item) => {
    // create thread for this notification then open composer
    try {
      const res = await createThread({ notificationId: item.id, title: item.title })
      const threadId = res.data.threadId
      onClose && onClose()
      window.location.assign(`/notifications/threads/${threadId}`)
    } catch (err) { console.error(err) }
  }

  if (!open) return null
  return (
    <div className="fixed right-6 top-16 w-80 bg-white border shadow-lg rounded-lg z-50 focus-ring fade-in-up" role="dialog" aria-modal="true" aria-label="Notifications" tabIndex={-1}>
      <div className="p-3 border-b flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="font-semibold">Notifications</div>
          <button className="btn btn-sm btn-ghost" onClick={async () => {
            // mark all as read client-side
            const unread = items.filter(i => !i.read)
            await Promise.all(unread.map(i => markDeliveryRead(i.deliveryId).catch(()=>{})))
            setItems(prev => prev.map(x => ({ ...x, read: true })))
          }} aria-label="Mark all as read">Mark all</button>
          <button className="btn btn-sm btn-ghost" onClick={() => { onClose && onClose(); window.location.assign('/notifications') }} aria-label="View all notifications">View all</button>
        </div>
        <div className="text-sm text-gray-500">Recent</div>
      </div>
      <div className="p-3 space-y-2 max-h-[56vh] overflow-y-auto">
        {loading ? (
          <div className="space-y-2">
            <div className="flex items-start gap-3">
              <div className="skeleton h-6 w-6 rounded-full"></div>
              <div className="flex-1 space-y-2">
                <div className="skeleton h-4 w-32"></div>
                <div className="skeleton h-4 w-full"></div>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <div className="skeleton h-6 w-6 rounded-full"></div>
              <div className="flex-1 space-y-2">
                <div className="skeleton h-4 w-32"></div>
                <div className="skeleton h-4 w-full"></div>
              </div>
            </div>
          </div>
        ) : items.length === 0 ? (
          <div className="text-sm text-gray-500">No notifications</div>
        ) : (
          items.map(i => (
            <NotificationCard key={i.deliveryId} item={i} onOpen={() => {}} onMarkRead={handleMarkRead} onReply={handleReply} onMute={handleMute} />
          ))
        )}
      </div>
      <div className="p-3 border-t text-right">
        <button onClick={onClose} className="btn btn-ghost" aria-label="Close notifications">Close</button>
      </div>
    </div>
  )
}
