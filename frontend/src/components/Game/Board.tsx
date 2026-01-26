import { Position } from '../../types/Position';
import { StoneOnBoard } from '../../types/Game';
import './Board.css';

interface TerritoryMarkers {
  blackTerritory: Position[];
  whiteTerritory: Position[];
}

interface BoardProps {
  size: number;
  stones: StoneOnBoard[];
  onIntersectionClick?: (position: Position) => void;
  disabled?: boolean;
  lastMove?: Position;
  territory?: TerritoryMarkers;
}

const Board = ({ size, stones, onIntersectionClick, disabled = false, lastMove, territory }: BoardProps) => {
  const cellSize = 40;
  const margin = 30;
  const boardSize = cellSize * (size - 1) + 2 * margin;

  // Calculate star points based on board size
  const getStarPoints = (): Position[] => {
    if (size === 19) {
      return [
        { x: 3, y: 3 }, { x: 9, y: 3 }, { x: 15, y: 3 },
        { x: 3, y: 9 }, { x: 9, y: 9 }, { x: 15, y: 9 },
        { x: 3, y: 15 }, { x: 9, y: 15 }, { x: 15, y: 15 }
      ];
    } else if (size === 13) {
      return [
        { x: 3, y: 3 }, { x: 9, y: 3 },
        { x: 6, y: 6 },
        { x: 3, y: 9 }, { x: 9, y: 9 }
      ];
    } else if (size === 9) {
      return [
        { x: 2, y: 2 }, { x: 6, y: 2 },
        { x: 4, y: 4 },
        { x: 2, y: 6 }, { x: 6, y: 6 }
      ];
    }
    return [];
  };

  const starPoints = getStarPoints();

  const getCoordinate = (index: number): number => {
    return margin + index * cellSize;
  };

  const handleClick = (x: number, y: number) => {
    if (!disabled && onIntersectionClick) {
      onIntersectionClick({ x, y });
    }
  };

  // Create a map for quick stone lookup
  const stoneMap = new Map<string, 'black' | 'white'>();
  stones.forEach(stone => {
    stoneMap.set(`${stone.position.x},${stone.position.y}`, stone.color);
  });

  // Create maps for territory lookup
  const blackTerritorySet = new Set<string>();
  const whiteTerritorySet = new Set<string>();
  if (territory) {
    territory.blackTerritory.forEach(pos => {
      blackTerritorySet.add(`${pos.x},${pos.y}`);
    });
    territory.whiteTerritory.forEach(pos => {
      whiteTerritorySet.add(`${pos.x},${pos.y}`);
    });
  }

  const isLastMove = (x: number, y: number): boolean => {
    return lastMove ? lastMove.x === x && lastMove.y === y : false;
  };

  return (
    <div className="board-container">
      <svg
        width={boardSize}
        height={boardSize}
        className="go-board"
      >
        {/* Board background */}
        <rect
          x={0}
          y={0}
          width={boardSize}
          height={boardSize}
          fill="#DCB35C"
        />

        {/* Grid lines - vertical */}
        {Array.from({ length: size }).map((_, i) => (
          <line
            key={`v-${i}`}
            x1={getCoordinate(i)}
            y1={margin}
            x2={getCoordinate(i)}
            y2={boardSize - margin}
            stroke="#000"
            strokeWidth="1"
          />
        ))}

        {/* Grid lines - horizontal */}
        {Array.from({ length: size }).map((_, i) => (
          <line
            key={`h-${i}`}
            x1={margin}
            y1={getCoordinate(i)}
            x2={boardSize - margin}
            y2={getCoordinate(i)}
            stroke="#000"
            strokeWidth="1"
          />
        ))}

        {/* Star points */}
        {starPoints.map((point, idx) => (
          <circle
            key={`star-${idx}`}
            cx={getCoordinate(point.x)}
            cy={getCoordinate(point.y)}
            r="4"
            fill="#000"
          />
        ))}

        {/* Territory markers */}
        {territory && Array.from({ length: size }).map((_, x) =>
          Array.from({ length: size }).map((_, y) => {
            const key = `${x},${y}`;
            const isBlackTerritory = blackTerritorySet.has(key);
            const isWhiteTerritory = whiteTerritorySet.has(key);

            if (!isBlackTerritory && !isWhiteTerritory) return null;

            return (
              <rect
                key={`territory-${x}-${y}`}
                x={getCoordinate(x) - cellSize / 4}
                y={getCoordinate(y) - cellSize / 4}
                width={cellSize / 2}
                height={cellSize / 2}
                fill={isBlackTerritory ? 'rgba(0, 0, 0, 0.4)' : 'rgba(255, 255, 255, 0.7)'}
                stroke={isBlackTerritory ? '#000' : '#666'}
                strokeWidth="1"
                rx="2"
                className="territory-marker"
              />
            );
          })
        )}

        {/* Click areas for intersections */}
        {Array.from({ length: size }).map((_, x) =>
          Array.from({ length: size }).map((_, y) => (
            <circle
              key={`click-${x}-${y}`}
              cx={getCoordinate(x)}
              cy={getCoordinate(y)}
              r={cellSize / 2 - 2}
              fill="transparent"
              className={`intersection ${disabled ? 'disabled' : ''}`}
              onClick={() => handleClick(x, y)}
            />
          ))
        )}

        {/* Stones */}
        {Array.from({ length: size }).map((_, x) =>
          Array.from({ length: size }).map((_, y) => {
            const stone = stoneMap.get(`${x},${y}`);
            if (!stone) return null;

            return (
              <g key={`stone-${x}-${y}`}>
                <circle
                  cx={getCoordinate(x)}
                  cy={getCoordinate(y)}
                  r={cellSize / 2 - 2}
                  fill={stone === 'black' ? '#000' : '#fff'}
                  stroke={stone === 'white' ? '#000' : 'none'}
                  strokeWidth="1"
                  className="stone"
                />
                {/* Last move marker */}
                {isLastMove(x, y) && (
                  <circle
                    cx={getCoordinate(x)}
                    cy={getCoordinate(y)}
                    r="6"
                    fill={stone === 'black' ? '#fff' : '#000'}
                    className="last-move-marker"
                  />
                )}
              </g>
            );
          })
        )}
      </svg>
    </div>
  );
};

export default Board;
