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
        ? 'w-16 h-16'
        : isVertical
            ? 'w-16 h-12'
            : 'w-12 h-16';

    const contentRotation = space.isCorner
        ? getCornerRotationAngle(position)
        : getRotationAngle(position);

  return (
    <div className={`${sizeClass} p-2 border-t-2 border-l-2 border-black flex flex-col items-center justify-center ${space.color}`}
         style={style}>
        <div style={{ transform: `rotate(${contentRotation}deg)` }}>
        {space.type === 'property' && (
            <div className="h-4 border-b-2" style={{ backgroundColor: space.color }}></div>
        )}
        {space.name ? (
        <h4 className="text-xs text-center">{space.name}</h4>
        ) : null}
      {space.type !== 'property' && (
        <img src={`/images/${space.type}.png`}  className="w-6 h-6" />
      )}
      {space.behavior ? (
        <p className="text-2xs text-center">{space.behavior}</p>
      ) : null}
      </div>
    </div>
  )
}