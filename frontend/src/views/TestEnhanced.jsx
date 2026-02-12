import React from 'react'

const TestEnhanced = () => {
  return (
    <div style={{padding: '20px', backgroundColor: 'lightblue', border: '2px solid blue'}}>
      <h1>🎉 ENHANCED ROOM BOOKING TEST COMPONENT</h1>
      <p>If you can see this, the enhanced component system is working!</p>
      <p>Current time: {new Date().toLocaleString()}</p>
    </div>
  )
}

export default TestEnhanced
