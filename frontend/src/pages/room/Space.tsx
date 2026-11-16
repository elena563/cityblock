interface Space {
    name: string;
    type: string;
    color: string;
    behavior: string;
    position: number;
    isCorner: boolean;
    style?: React.CSSProperties;
}

export default function Space({ space, style }: { space: Space; style?: React.CSSProperties }) {

    function getRotationAngle(position: number): number {
        if ((position >= 0 && position <= 10) || position === 20 || position === 30) {
            return 0;
        } else if (position >= 11 && position <= 19) {
            return 90;
        } else if (position >= 21 && position <= 29) {
            return 180;
        } else {
            return 270;
        }
    }

    function getCornerRotationAngle(position: number): number {
        if (position === 0) {
            return 315;  
        } else if (position === 10) {
            return 45; 
        }
        else if (position === 20) {
            return 135; 
        }
        else if (position === 30) {
            return 225; 
        }
    }

    const position = space.position;
    const isVertical = (position > 10 && position < 20) || position > 30;

    const sizeClass = space.isCorner
        ? 'w-20 h-20'
        : isVertical
            ? 'w-20 h-12'
            : 'w-12 h-20';

    const innerSize = space.isCorner
        ? 'w-[65%] h-full'
        : 'w-12 h-20';

    const rotation = space.isCorner ? getCornerRotationAngle(position) : getRotationAngle(position);

    const borderClass = 
        position <= 10 ? 'border-t-2 border-l-2 border-b-2' :
        position <= 20 ? 'border-l-2 border-t-2' :  
        position <= 30 ? 'border-t-2 border-l-2' :
        'border-t-2 border-l-2'; 

    const imageName = space.type === 'metroStation' || space.type === 'tax' || space.type === 'utility' ? space.type.toLowerCase() + space.position : space.type.toLowerCase();
    const imageHeight = space.type === 'trouble' || space.type === 'chance' ? 'h-10' : 'h-8';
    const imageWidth = space.type === 'go' || space.type === 'chance' ? 'w-12' : 'w-8';

return (
    <div
        className={`${sizeClass} bg-card relative ${borderClass} border-solid border-black flex flex-col items-center justify-center overflow-hidden`}
        style={style}
    >
        <div
            className={`${innerSize} flex flex-col shrink-0`}
            style={{ transform: `rotate(${rotation}deg)` }}
        >
            {space.type === 'property' && (
                <div className="w-full h-4 shrink-0 border-b-2 border-black" style={{ backgroundColor: space.color }} />
            )}
            <div className="flex flex-1 min-h-0 flex-col items-center justify-between gap-1 px-1 py-1">
                {space.name ? (
                    <h4 className="text-2xs text-center leading-tight break-words">{space.name}</h4>
                ) : null}
                {space.type !== 'property' && (
                    <img src={`/images/${imageName}.png`} className={` ${imageWidth} ${imageHeight} overflow-auto`} />
                )}
                {space.behavior ? (
                    <p className="text-2xs text-center leading-tight break-words">{space.behavior}</p>
                ) : null}
            </div>
        </div>
    </div>
);
}