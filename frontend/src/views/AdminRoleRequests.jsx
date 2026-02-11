import React, { useEffect, useState } from 'react'
import api from '../lib/api'
import { useAuth } from '../lib/AuthContext'

export default function AdminRoleRequests() {
  const { hasRole } = useAuth()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await api.get('/api/admin/role-requests')
      setRows(Array.isArray(res.data) ? res.data : [])
    } catch (e) {
      setError('Failed to load requests')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const act = async (id, action) => {
    setError('')
    setMsg('')
    try {
      await api.post(`/api/admin/role-requests/${id}/${action}`)
      setMsg(action === 'approve' ? 'Approved' : 'Rejected')
      setRows(rows.filter(r => r.id !== id))
    } catch (e) {
      setError('Action failed')
    }
  }

  if (!hasRole('ADMIN')) return null

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Role Requests</h1>
        <p className="text-gray-600">Approve or reject requested roles.</p>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}
      {msg && <div className="alert alert-success mb-4">{msg}</div>}

      {loading ? (
        <div className="text-gray-600">Loading...</div>
      ) : rows.length === 0 ? (
        <div className="text-gray-600">No pending requests.</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full bg-white border border-gray-200 rounded-lg">
            <thead>
              <tr className="bg-gray-50 text-left">
                <th className="px-4 py-3 border-b">Username</th>
                <th className="px-4 py-3 border-b">Email</th>
                <th className="px-4 py-3 border-b">Requested Role</th>
                <th className="px-4 py-3 border-b">Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map(r => (
                <tr key={r.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 border-b">{r.username}</td>
                  <td className="px-4 py-3 border-b">{r.email}</td>
                  <td className="px-4 py-3 border-b">{r.requestedRole}</td>
                  <td className="px-4 py-3 border-b">
                    <div className="flex items-center gap-2">
                      <button className="btn btn-primary btn-sm" onClick={() => act(r.id, 'approve')}>Approve</button>
                      <button className="btn btn-secondary btn-sm" onClick={() => act(r.id, 'reject')}>Reject</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
