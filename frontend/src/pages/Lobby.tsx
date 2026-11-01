import { useState } from 'react'
import { useNavigate } from 'react-router'
import { Input } from '@/components/ui/input.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Field, FieldGroup } from '@/components/ui/field.tsx'

export default function Lobby() {
  type LobbyRole = 'creator' | 'joiner'
  const [role, ] = useState<LobbyRole>('creator') // will be determined by backend in the future
  const players = ['Elena', 'Marco', null, null, null, null]
  const navigate = useNavigate()

  const handleStarted = () => {
    navigate('/room/ABC123')
  }

  return (
    <main className="flex flex-col items-center justify-center min-h-screen bg-background">
      <div className="flex flex-col items-center justify-center bg-card p-6 rounded-lg shadow-lg">
      {role === 'creator' ? 
      <>
        <h2 className="text-2xl text-foreground">Now you just have to go call your friends</h2>
        <p className="mt-4 w-full">Share this room code with them:</p>
        <p className="text-center text-3xl my-2 w-full font-bold text-primary">ABC123</p>
        <p className="w-full">You can start the game as soon as at least one other player joins the room</p>
        <FieldGroup className="grid grid-cols-1 sm:grid-cols-2 gap-4 w-full my-4">
          {players.map((player, index) => (
            <Field key={index} className="flex flex-col items-center justify-center">
              <Input
                value={player || `Player ${index + 1}`}
                disabled
                className={`text-center ${!player ? 'bg-muted' : 'bg-success/10 !text-foreground disabled:opacity-100'}`}
              />
            </Field>
          ))}
        </FieldGroup>
        <Button className="w-full" onClick={handleStarted}>Start Game</Button>
      </> 
      : 
      <>
        <h2 className="text-2xl text-foreground">You're in! <br />Now you just have to wait for a while</h2>
        <p className="text-left mt-4 w-full">The game will start as soon as the host confirms</p>
      </>
      }
        
      </div>
    </main>
  )
}