import React from 'react'

export default function Skeleton({ className = '', width = '100%', height = '1rem', style = {} }) {
  const s = { width, height, ...style }
  return <div className={`skeleton animate-pulse ${className}`} style={s} role="status" aria-busy="true" />
}
