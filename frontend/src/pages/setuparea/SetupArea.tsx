import { useState } from 'react'
import { useNavigate } from 'react-router'
import RegisterForm from './components/RegisterForm'
import RoomSelector from './components/RoomSelector'

export default function SetupArea() {
  const [phase, setPhase] = useState<'register'|'roomSelection'>('roomSelection')
  const navigate = useNavigate()

  // TODO: Implement actual registration and room joining logic when backend ready
  const handleRegistered = () => {
    localStorage.setItem('session_token', 'fake-token')
    setPhase('roomSelection')
  }

  const handleRoomJoined = () => {
    navigate('/lobby/ABC123')
  }

  return (
    <main className="flex flex-col items-center justify-center min-h-screen bg-background">
      {phase === 'register'
        ? <RegisterForm onRegistered={handleRegistered} />
        : <RoomSelector onRoomJoined={handleRoomJoined} />
      }
    </main>
  )
}