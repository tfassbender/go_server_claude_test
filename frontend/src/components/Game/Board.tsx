import { Position } from '../../types/Position';
import { StoneOnBoard } from '../../types/Game';
import './Board.css';

interface TerritoryMarkers {
  blackTerritory: Position[];
  whiteTerritory: Position[];
}

interface DeadStonesMarkers {
  blackDeadStones: Position[];
  whiteDeadStones: Position[];
}

interface BoardProps {
  size: number;
  stones: StoneOnBoard[];
  onIntersectionClick?: (position: Position) => void;
  disabled?: boolean;
  lastMove?: Position;
  territory?: TerritoryMarkers;
  fixMode?: boolean;
  markedDeadStones?: Position[];
  onStoneClick?: (position: Position) => void;
  deadStones?: DeadStonesMarkers;
  showMoveNumbers?: boolean;
  moveNumberMap?: Map<string, number>;
}

const Board = ({
  size,
  stones,
  onIntersectionClick,
  disabled = false,
  lastMove,
  territory,
  fixMode = false,
  markedDeadStones = [],
  onStoneClick,
  deadStones,
  showMoveNumbers = false,
  moveNumberMap
}: BoardProps) => {
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
    if (fixMode && onStoneClick) {
      // In fix mode, clicking a stone toggles its dead status
      const stone = stoneMap.get(`${x},${y}`);
      if (stone) {
        onStoneClick({ x, y });
      }
      return;
    }
    if (!disabled && onIntersectionClick) {
      onIntersectionClick({ x, y });
    }
  };

  // Create a set for quick dead stone lookup
  const deadStoneSet = new Set<string>();
  markedDeadStones.forEach(pos => {
    deadStoneSet.add(`${pos.x},${pos.y}`);
  });

  const isMarkedDead = (x: number, y: number): boolean => {
    return deadStoneSet.has(`${x},${y}`);
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

  // Create sets for dead stones (from game result)
  const blackDeadStoneSet = new Set<string>();
  const whiteDeadStoneSet = new Set<string>();
  if (deadStones) {
    deadStones.blackDeadStones.forEach(pos => {
      blackDeadStoneSet.add(`${pos.x},${pos.y}`);
    });
    deadStones.whiteDeadStones.forEach(pos => {
      whiteDeadStoneSet.add(`${pos.x},${pos.y}`);
    });
  }

  const isDeadStone = (x: number, y: number): boolean => {
    const key = `${x},${y}`;
    return blackDeadStoneSet.has(key) || whiteDeadStoneSet.has(key);
  };

  // Determine cross color based on territory: stones in black territory get black cross, etc.
  const getDeadStoneMarkerColor = (x: number, y: number): string => {
    const key = `${x},${y}`;
    if (blackTerritorySet.has(key)) {
      return '#000';
    } else if (whiteTerritorySet.has(key)) {
      return '#fff';
    }
    // Fallback: use opposite of stone color
    const stone = stoneMap.get(key);
    return stone === 'black' ? '#fff' : '#000';
  };

  const isLastMove = (x: number, y: number): boolean => {
    return lastMove ? lastMove.x === x && lastMove.y === y : false;
  };

  return (
    <div className={`board-container${fixMode ? ' fix-mode' : ''}`}>
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

            const cx = getCoordinate(x);
            const cy = getCoordinate(y);
            const isDead = isMarkedDead(x, y);

            return (
              <g key={`stone-${x}-${y}`} className={fixMode ? 'stone-group-clickable' : ''}>
                <circle
                  cx={cx}
                  cy={cy}
                  r={cellSize / 2 - 2}
                  fill={stone === 'black' ? '#000' : '#fff'}
                  stroke={stone === 'white' ? '#000' : 'none'}
                  strokeWidth="1"
                  className={`stone${fixMode ? ' fix-mode-stone' : ''}`}
                  onClick={fixMode ? () => handleClick(x, y) : undefined}
                />
                {/* Last move marker (only show if not showing move numbers) */}
                {isLastMove(x, y) && !showMoveNumbers && (
                  <circle
                    cx={cx}
                    cy={cy}
                    r="6"
                    fill={stone === 'black' ? '#fff' : '#000'}
                    className="last-move-marker"
                  />
                )}
                {/* Move number */}
                {showMoveNumbers && moveNumberMap && moveNumberMap.has(`${x},${y}`) && (
                  <text
                    x={cx}
                    y={cy}
                    textAnchor="middle"
                    dominantBaseline="central"
                    fill={stone === 'black' ? '#fff' : '#000'}
                    fontSize="12"
                    fontWeight="bold"
                    className="move-number"
                  >
                    {moveNumberMap.get(`${x},${y}`)}
                  </text>
                )}
                {/* Dead stone marker (X) - fix mode (red) */}
                {fixMode && isDead && (
                  <g className="dead-stone-marker">
                    <line
                      x1={cx - 8}
                      y1={cy - 8}
                      x2={cx + 8}
                      y2={cy + 8}
                      stroke="red"
                      strokeWidth="3"
                    />
                    <line
                      x1={cx + 8}
                      y1={cy - 8}
                      x2={cx - 8}
                      y2={cy + 8}
                      stroke="red"
                      strokeWidth="3"
                    />
                  </g>
                )}
                {/* Dead stone marker (X) - game result (territory-colored) */}
                {!fixMode && deadStones && isDeadStone(x, y) && (
                  <g className="dead-stone-marker">
                    <line
                      x1={cx - 8}
                      y1={cy - 8}
                      x2={cx + 8}
                      y2={cy + 8}
                      stroke={getDeadStoneMarkerColor(x, y)}
                      strokeWidth="3"
                    />
                    <line
                      x1={cx + 8}
                      y1={cy - 8}
                      x2={cx - 8}
                      y2={cy + 8}
                      stroke={getDeadStoneMarkerColor(x, y)}
                      strokeWidth="3"
                    />
                  </g>
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
