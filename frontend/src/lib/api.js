import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Retry once on transient transport errors (e.g., HTTP/2 GOAWAY during server restart)
api.interceptors.response.use(undefined, async (error) => {
  const cfg = error?.config || {}
  const msg = String(error?.message || '')
  const isTransient = msg.includes('server_shutting_down') || msg.includes('GOAWAY') || (!error.response && error.request)
  if (isTransient && !cfg._retry) {
    cfg._retry = true
    await new Promise(r => setTimeout(r, 500))
    return api.request(cfg)
  }

  const status = error?.response?.status
  if (status === 401 || status === 403) {
    try {
      localStorage.removeItem('token')
    } catch {
      // ignore
    }
    if (window.location.pathname !== '/login') {
      window.location.assign('/login')
    }
  }
  return Promise.reject(error)
})

export default api


