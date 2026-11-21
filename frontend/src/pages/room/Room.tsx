import Board from "./Board";
import PlayersList from "./PlayersList";
import TurnPanel from "./TurnPanel";
import HistoryLog from "./HistoryLog";

// temporary data for the list of players
const players = [
    { name: "Alice", color: "red", balance: 1500 },
    { name: "Bob", color: "blue", balance: 1500 },
    { name: "Charlie", color: "green", balance: 1500 },
    { name: "David", color: "yellow", balance: 1500 },
]

export default function Room() {
  return (
    <div className="h-screen overflow-hidden flex gap-4 p-4">
      <aside className="hidden md:flex md:w-63 shrink-0 flex-col overflow-y-auto">
        <div className="h-full flex flex-col justify-between gap-4">
          <HistoryLog />
          <TurnPanel />
        </div>
      </aside>

      <main className="flex-1 min-w-0 min-h-0 flex items-center justify-center board-area">
        <Board />
      </main>

      <aside className="hidden md:flex md:w-63 shrink-0 flex-col overflow-y-auto pb-16">
        <PlayersList players={players} />
      </aside>
    </div>
  )
}