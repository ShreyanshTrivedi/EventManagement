import React, { useEffect, useState } from 'react'
import { fetchEventNotifications, createThread, fetchThreadMessages, postThreadMessage, postEventNotification } from '../lib/api'

export default function EventNotificationsPanel({ eventId, canView, canPost }) {
  const [items, setItems] = useState([])
  const [activeThread, setActiveThread] = useState(null)
  const [threadMessages, setThreadMessages] = useState([])
  const [replyText, setReplyText] = useState('')

  // composer state (event owners / admin)
  const [title, setTitle] = useState('')
  const [messageText, setMessageText] = useState('')
  const [urgency, setUrgency] = useState('NORMAL')
  const [threadEnabledFlag, setThreadEnabledFlag] = useState(false)
  const [posting, setPosting] = useState(false)
  const [postMsg, setPostMsg] = useState('')

  useEffect(() => {
    if (!canView) return
    fetchEventNotifications(eventId).then(res => setItems(res.data || [])).catch(() => setItems([]))
  }, [eventId, canView])

  const openDiscussion = async (item) => {
    try {
      const res = await createThread({ notificationId: item.id, title: item.title })
      const threadId = res.data.threadId
      setActiveThread({ id: threadId, title: item.title })
      const msgs = await fetchThreadMessages(threadId)
      setThreadMessages(msgs.data || [])
    } catch (err) { console.error(err) }
  }

  const sendReply = async () => {
    if (!activeThread || !replyText.trim()) return
    await postThreadMessage(activeThread.id, { content: replyText })
    const msgs = await fetchThreadMessages(activeThread.id)
    setThreadMessages(msgs.data || [])
    setReplyText('')
  }

  const postNotification = async () => {
    if (!title.trim() || !messageText.trim()) {
      setPostMsg('Title and message are required')
      return
    }
    setPosting(true)
    setPostMsg('')
    try {
      await postEventNotification(eventId, { title: title.trim(), message: messageText.trim(), urgency, threadEnabled: threadEnabledFlag })
      setTitle('')
      setMessageText('')
      setThreadEnabledFlag(false)
      setPostMsg('Posted')
      const res = await fetchEventNotifications(eventId)
      setItems(res.data || [])
      // UX feedback
      const toastEvt = new CustomEvent('app-toast', { detail: { id: Date.now(), message: 'Notification posted', type: 'success', timeout: 3500 } })
      window.dispatchEvent(toastEvt)
    } catch (err) {
      console.error(err)
      setPostMsg('Failed to post notification')
      const toastEvt = new CustomEvent('app-toast', { detail: { id: Date.now(), message: 'Failed to post notification', type: 'error', timeout: 3500 } })
      window.dispatchEvent(toastEvt)
    } finally {
      setPosting(false)
    }
  }

  if (!canView) return null
  return (
    <div className="card mt-6">
      <h3 className="text-lg font-semibold mb-4">Event Notifications</h3>
      {canPost && (
        <div className="mb-4 card card-compact" aria-live="polite">
          <div className="space-y-2">
            <input value={title} onChange={e => setTitle(e.target.value)} placeholder="Notification title" className="form-input" aria-label="Notification title" aria-invalid={!title.trim()} />
            {!title.trim() && <div className="text-xs text-red-600">Title is required</div>}
            <textarea value={messageText} onChange={e => setMessageText(e.target.value)} placeholder="Message (visible to recipients)" className="form-textarea" rows={2} aria-label="Notification message" aria-invalid={!messageText.trim()} />
            {!messageText.trim() && <div className="text-xs text-red-600">Message is required</div>}
            <div className="flex items-center gap-3">
              <select value={urgency} onChange={e => setUrgency(e.target.value)} className="form-select" aria-label="Urgency">
                <option value="LOW">Low</option>
                <option value="NORMAL">Normal</option>
                <option value="HIGH">High</option>
              </select>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={threadEnabledFlag} onChange={e => setThreadEnabledFlag(e.target.checked)} />
                Enable discussion
              </label>
              <button className="btn btn-primary btn-sm" onClick={postNotification} disabled={posting || !title.trim() || !messageText.trim()} aria-disabled={posting || !title.trim() || !messageText.trim()}>{posting ? 'Posting...' : 'Post'}</button>
              {postMsg && <div className="text-sm text-gray-600 ml-2">{postMsg}</div>}
            </div>
          </div>
        </div>
      )}
      {items.length === 0 && <div className="text-sm text-gray-500">No notifications for this event.</div>}
      <div className="space-y-3">
        {items.map(it => (
          <div key={it.deliveryId} className="p-3 border rounded-lg bg-white/50 fade-in-up interactive">
            <div className="flex justify-between items-start">
              <div>
                <div className="font-semibold">{it.title}</div>
                <div className="text-sm text-slate-600 mt-1">{it.message}</div>
                <div className="text-xs text-slate-400 mt-2">{new Date(it.createdAt).toLocaleString()}</div>
              </div>
              <div className="flex flex-col gap-2 items-end">
                {it.threadEnabled && <button onClick={() => openDiscussion(it)} className="btn btn-ghost btn-sm">Discuss</button>}
                {!it.read && <span className="text-xs text-indigo-600">New</span>}
              </div>
            </div>
          </div>
        ))}
      </div>

      {activeThread && (
        <div className="mt-4 border-t pt-4">
          <div className="font-semibold mb-2">Discussion — {activeThread.title}</div>
          <div className="space-y-3 max-h-56 overflow-y-auto mb-3">
            {threadMessages.map(m => (
              <div key={m.id} className="text-sm">
                <div className="text-xs text-gray-400">{m.author} • {new Date(m.createdAt).toLocaleString()}</div>
                <div className="mt-1">{m.content}</div>
              </div>
            ))}
          </div>
          <div className="flex gap-2">
            <input value={replyText} onChange={e => setReplyText(e.target.value)} className="form-input flex-1" placeholder="Write a reply..." />
            <button onClick={sendReply} className="btn btn-primary">Send</button>
          </div>
        </div>
      )}
    </div>
  )
}
