import './App.css'
import { Routes, Route, Navigate } from 'react-router'
import SetupArea from './pages/setuparea/SetupArea.tsx'
import Lobby from './pages/Lobby.tsx'
import Game from './pages/Game.tsx'

function App() {

  return (
    <div className="min-h-screen bg-background">
      <Routes>
        <Route path="/" element={<SetupArea />} />
        <Route path="/lobby/:roomCode" element={<Lobby />} />
        <Route path="/game/:roomCode" element={<Game />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
      <img src="/images/cityblock-logo.webp" alt="CityBlock Logo" className="mb-4 mr-4 fixed bottom-0 right-0 w-32 pointer-events-none" />
    </div>
  )
}

export default App
