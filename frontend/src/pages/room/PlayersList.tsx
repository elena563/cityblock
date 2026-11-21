interface Player {
    name: string;
    balance: number;
    color: string;
}

export default function PlayersList({ players }: { players: Player[] }) {  
    return (
        <div className="flex flex-col gap-2 p-4 bg-card border-2 border-black rounded-lg">
            <h2 className="text-lg font-bold mb-2 text-center">Players List</h2>
            <div className="flex justify-between gap-2 mb-2">
                <p className="text-sm font-semibold">name</p>
                <p className="text-sm font-semibold">balance</p>
            </div>
            {players.map((player, index) => (
                <div key={index} className="flex items-center gap-2">
                    <span>{player.name}</span>
                    <span className="ml-auto">€{player.balance}</span>
                </div>
            ))}
        </div>
    );
}