import { useState } from 'react'
import RegisterForm from './components/RegisterForm'
import RoomSelector from './components/RoomSelector'

export default function SetupArea() {

  const [phase, ] = useState<'register'|'roomSelection'>('roomSelection')

  return (
    <main className="flex flex-col items-center justify-center min-h-screen bg-background">
      {phase === 'register' ? <RegisterForm /> :  <RoomSelector />}
    </main>
  )
}