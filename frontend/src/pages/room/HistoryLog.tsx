export default function HistoryLog() {  
    const turns = [
        { player: "Alice", action: "rolled a 6" },
        { player: "Bob", action: "bought a property" },
    ]
    return (
        <div className="flex flex-col gap-2 p-4 bg-card border-2 border-black rounded-lg">
            <h2 className="text-lg font-bold mb-2 text-center">Actions Log</h2>
            <div className="rounded-lg border border-muted-foreground p-2 mb-2">
            {turns.map((turn, index) => (
                <div key={index} className="flex items-center gap-2">
                    <span>{turn.player} {turn.action}</span>
                </div>
            ))}
            </div>
        </div>
    );
}