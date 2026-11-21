export default function TurnPanel() { 

    const turns = [
        { player: "Alice", color: "red" },
        { player: "Bob", color: "blue" },
    ]
    //TODO: handle current turn and highlight the current player in the list, link piece

    return (
        <div className="flex flex-col gap-2 p-4 bg-card border-2 border-black rounded-lg">
            <h2 className="text-lg font-bold mb-2 text-center">Turn Order</h2>
            {turns.map((turn, index) => (
                <div key={index} className="flex items-center gap-2">
                 <div className="w-8 h-8 rounded-full" style={{ backgroundColor: turn.color }}>
                        <span className="text-white text-sm font-bold">{turn.player.charAt(0)}</span>
                    </div>
                    <span>{turn.player}</span>
                </div>
            ))}
        </div>
    );
}