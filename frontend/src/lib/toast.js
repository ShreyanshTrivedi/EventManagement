export function showToast({ message = '', type = 'info', timeout = 4000 } = {}) {
  if (!message) return
  const evt = new CustomEvent('app-toast', { detail: { id: Date.now(), message, type, timeout } })
  window.dispatchEvent(evt)
}

export default showToast
