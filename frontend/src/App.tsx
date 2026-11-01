import './App.css'
import { Routes, Route, Navigate } from 'react-router'
import SetupArea from './pages/SetupArea.tsx'
import Lobby from './pages/Lobby.tsx'
import Game from './pages/Game.tsx'

function App() {

  return (
    <Routes>
      <Route path="/" element={<SetupArea />} />
      <Route path="/lobby/:roomCode" element={<Lobby />} />
      <Route path="/game/:roomCode" element={<Game />} />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

export default App
