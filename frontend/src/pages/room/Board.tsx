import Space from "./Space";

// temporary data for spaces on the board prototype
const spaces = [
    { name: null, type: "GO", color: "red", behavior: "Collect $200", position: 0, isCorner: true },
    { name: "Main Street", type: "property", color: "blue", behavior: null, position: 1, isCorner: false },
    { name: "City Hall", type: "CITY_HALL", color: "blue", behavior: null, position: 2, isCorner: false },
    { name: "Broadway", type: "property", color: "blue", behavior: null, position: 3, isCorner: false },
    { name: null, type: "transit", color: "red", behavior: "Collect $200", position: 4, isCorner: false },
    { name: "5th Avenue", type: "property", color: "green", behavior: null, position: 5, isCorner: false },
    { name: "Central Park", type: "CITY_PARK", color: "green", behavior: null, position: 6, isCorner: false },
    { name: "Madison Avenue", type: "property", color: "green", behavior: null, position: 7, isCorner: false },
    { name: null, type: "transit", color: "red", behavior: "Collect $200", position: 8, isCorner: false },
    { name: "Wall Street", type: "property", color: "yellow", behavior: null, position: 9, isCorner: false },
    { name: null, type: "transit", color: "red", behavior: "Collect $200", position: 10, isCorner: true },
    { name: "5th Avenue", type: "property", color: "green", behavior: null, position: 11, isCorner: false },
    { name: "Central Park", type: "CITY_PARK", color: "green", behavior: null, position: 12, isCorner: false },
    { name: "Madison Avenue", type: "property", color: "green", behavior: null, position: 13, isCorner: false },
    { name: null, type: "transit", color: "red", behavior: "Collect $200", position: 14, isCorner: false },
    { name: "Wall Street", type: "property", color: "yellow", behavior: null, position: 15, isCorner: false },
    { name: null, type: "transit", color: "red", behavior: "Collect $200", position: 16, isCorner: false },
    { name: "Main Street", type: "property", color: "blue", behavior: null, position: 17, isCorner: false },
    { name: "City Hall", type: "CITY_HALL", color: "blue", behavior: null, position: 18, isCorner: false },
    { name: "Broadway", type: "property", color: "blue", behavior: null, position: 19, isCorner: false },
    { name: "City Park", type: "CITY_PARK", color: "red", behavior: null, position: 20, isCorner: true },
    { name: "Wall Street", type: "property", color: "yellow", behavior: null, position: 21, isCorner: false },
    { name: "Stock Exchange", type: "STOCK_EXCHANGE", color: "yellow", behavior: null, position: 22, isCorner: false },
    { name: "Broad Street", type: "property", color: "yellow", behavior: null, position: 23, isCorner: false },
    { name: null, type: "transit", color: "red", behavior: "Collect $200", position: 24, isCorner: false },
    { name: "5th Avenue", type: "property", color: "green", behavior: null, position: 25, isCorner: false },
    { name: "Central Park", type: "CITY_PARK", color: "green", behavior: null, position: 26, isCorner: false },
    { name: "Madison Avenue", type: "property", color: "green", behavior: null, position: 27, isCorner: false },
    { name: null, type: "transit", color: "red", behavior: "Collect $200", position: 28, isCorner: false },
    { name: "Wall Street", type: "property", color: "yellow", behavior: null, position: 29, isCorner: false },
    { name: null, type: "GO", color: "red", behavior: "Collect $200", position: 30, isCorner: true },
    { name: "Main Street", type: "property", color: "blue", behavior: null, position: 31, isCorner: false },
    { name: "City Hall", type: "CITY_HALL", color: "blue", behavior: null, position: 32, isCorner: false },
    { name: "Broadway", type: "property", color: "blue", behavior: null, position: 33, isCorner: false },
    { name: null, type: "transit", color: "red", behavior: "Collect $200", position: 34, isCorner: false },
    { name: "5th Avenue", type: "property", color: "green", behavior: null, position: 35, isCorner: false },
    { name: "Central Park", type: "CITY_PARK", color: "green", behavior: null, position: 36, isCorner: false },
    { name: "Madison Avenue", type: "property", color: "green", behavior: null, position: 37, isCorner: false },
    { name: null, type: "transit", color: "red", behavior: "Collect $200", position: 38, isCorner: false },
    { name: "Wall Street", type: "property", color: "yellow", behavior: null, position: 39, isCorner: false },
];

export default function Board() {

    function getGridPosition(index: number): { row: number; col: number } {
        if (index <= 10) return { row: 11, col: 11 - index };        // bottom
        if (index <= 20) return { row: 11 - (index - 10), col: 1 };  // left
        if (index <= 30) return { row: 1, col: index - 19 };          // top
        return { row: index - 29, col: 11 };                          // right
    }

  return (
    <div className="grid max-h-screen border-r-2 border-b-2 border-black" style={{ gridTemplateColumns: '4rem repeat(9, 3rem) 4rem', gridTemplateRows: '4rem repeat(9, 3rem) 4rem' }}>
        {spaces.map((space, i) => {
            const { row, col } = getGridPosition(i);
            return (
            <Space
                key={i}
                space={space}
                style={{ gridRow: row, gridColumn: col }}
            />
            );
        })}
        <div className="border-t-2 border-l-2 border-black" style={{ gridRow: '2 / 11', gridColumn: '2 / 11' }}></div>
    </div>
  )
}