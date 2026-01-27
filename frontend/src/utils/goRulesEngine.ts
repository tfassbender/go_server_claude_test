import { Position } from '../types/Position';

/**
 * Client-side Go rules engine for validating and executing moves.
 * Used for the fork/sandbox feature in game analysis.
 */

export type StoneColor = 'black' | 'white';

/**
 * Result of attempting to execute a move.
 */
export interface MoveResult {
  success: boolean;
  error?: string;
  capturedStones: Position[];
  newBoardHash: string;
}

/**
 * Converts a position to a string key for map storage.
 */
function posKey(pos: Position): string {
  return `${pos.x},${pos.y}`;
}

/**
 * Parses a position key back to a Position object.
 */
function parseKey(key: string): Position {
  const [x, y] = key.split(',').map(Number);
  return { x, y };
}

/**
 * Go board representation with move validation logic.
 */
export class GoBoard {
  private grid: Map<string, StoneColor>;
  private size: number;

  constructor(size: number) {
    this.grid = new Map();
    this.size = size;
  }

  /**
   * Creates a deep copy of this board.
   */
  clone(): GoBoard {
    const copy = new GoBoard(this.size);
    copy.grid = new Map(this.grid);
    return copy;
  }

  /**
   * Gets the stone color at a position, or null if empty.
   */
  getStone(pos: Position): StoneColor | null {
    return this.grid.get(posKey(pos)) ?? null;
  }

  /**
   * Sets a stone at a position.
   */
  setStone(pos: Position, color: StoneColor): void {
    this.grid.set(posKey(pos), color);
  }

  /**
   * Removes a stone from a position.
   */
  removeStone(pos: Position): void {
    this.grid.delete(posKey(pos));
  }

  /**
   * Checks if a position is empty.
   */
  isEmpty(pos: Position): boolean {
    return !this.grid.has(posKey(pos));
  }

  /**
   * Checks if a position is within board bounds.
   */
  isValidPosition(pos: Position): boolean {
    return pos.x >= 0 && pos.x < this.size && pos.y >= 0 && pos.y < this.size;
  }

  /**
   * Gets the four adjacent positions (not diagonal).
   */
  getAdjacentPositions(pos: Position): Position[] {
    const deltas = [
      { x: -1, y: 0 },
      { x: 1, y: 0 },
      { x: 0, y: -1 },
      { x: 0, y: 1 },
    ];
    return deltas
      .map((d) => ({ x: pos.x + d.x, y: pos.y + d.y }))
      .filter((p) => this.isValidPosition(p));
  }

  /**
   * Gets all positions in the same group as the given position.
   * Uses flood fill to find connected stones of the same color.
   */
  getGroup(pos: Position): Set<string> {
    const color = this.getStone(pos);
    if (!color) return new Set();

    const group = new Set<string>();
    const toVisit: Position[] = [pos];

    while (toVisit.length > 0) {
      const current = toVisit.pop()!;
      const key = posKey(current);

      if (group.has(key)) continue;
      if (this.getStone(current) !== color) continue;

      group.add(key);

      for (const adj of this.getAdjacentPositions(current)) {
        if (!group.has(posKey(adj))) {
          toVisit.push(adj);
        }
      }
    }

    return group;
  }

  /**
   * Counts the number of liberties for a group of stones.
   */
  countLiberties(group: Set<string>): number {
    const liberties = new Set<string>();

    for (const key of group) {
      const pos = parseKey(key);
      for (const adj of this.getAdjacentPositions(pos)) {
        if (this.isEmpty(adj)) {
          liberties.add(posKey(adj));
        }
      }
    }

    return liberties.size;
  }

  /**
   * Generates a hash of the current board state.
   * Used for Ko detection.
   */
  getBoardHash(): string {
    const entries: string[] = [];
    for (const [key, color] of this.grid.entries()) {
      entries.push(`${key}:${color}`);
    }
    entries.sort();
    return entries.join(';');
  }

  /**
   * Returns the board size.
   */
  getSize(): number {
    return this.size;
  }

  /**
   * Returns all stones on the board.
   */
  getAllStones(): Array<{ position: Position; color: StoneColor }> {
    const stones: Array<{ position: Position; color: StoneColor }> = [];
    for (const [key, color] of this.grid.entries()) {
      stones.push({ position: parseKey(key), color });
    }
    return stones;
  }
}

/**
 * Gets the opposite color.
 */
export function oppositeColor(color: StoneColor): StoneColor {
  return color === 'black' ? 'white' : 'black';
}

/**
 * Validates and executes a move on the board.
 * Returns the result with captured stones or an error message.
 *
 * @param board - The current board state (will be cloned, not modified)
 * @param position - Where to place the stone
 * @param color - The color of the stone to place
 * @param koBlockedHash - The board hash that would be blocked by Ko rule (or null)
 * @returns MoveResult with success status and captured stones
 */
export function validateAndExecuteMove(
  board: GoBoard,
  position: Position,
  color: StoneColor,
  koBlockedHash: string | null
): MoveResult {
  // 1. Check position is within board bounds
  if (!board.isValidPosition(position)) {
    return {
      success: false,
      error: 'Position is outside the board',
      capturedStones: [],
      newBoardHash: board.getBoardHash(),
    };
  }

  // 2. Check intersection is empty
  if (!board.isEmpty(position)) {
    return {
      success: false,
      error: 'Position is already occupied',
      capturedStones: [],
      newBoardHash: board.getBoardHash(),
    };
  }

  // 3. Simulate placing the stone
  const testBoard = board.clone();
  testBoard.setStone(position, color);

  // 4. Detect and remove captured opponent groups
  const capturedStones: Position[] = [];
  const opponentColor = oppositeColor(color);

  for (const adj of testBoard.getAdjacentPositions(position)) {
    if (testBoard.getStone(adj) === opponentColor) {
      const group = testBoard.getGroup(adj);
      if (testBoard.countLiberties(group) === 0) {
        // Capture this group
        for (const key of group) {
          const capturedPos = parseKey(key);
          capturedStones.push(capturedPos);
          testBoard.removeStone(capturedPos);
        }
      }
    }
  }

  // 5. Check suicide (placed stone's group must have liberties or have captured something)
  const placedGroup = testBoard.getGroup(position);
  if (testBoard.countLiberties(placedGroup) === 0 && capturedStones.length === 0) {
    return {
      success: false,
      error: 'Suicide is not allowed',
      capturedStones: [],
      newBoardHash: board.getBoardHash(),
    };
  }

  // 6. Check Ko (new board state must not match ko-blocked hash)
  const newHash = testBoard.getBoardHash();
  if (koBlockedHash && newHash === koBlockedHash) {
    return {
      success: false,
      error: 'Ko rule violation - cannot recreate previous position',
      capturedStones: [],
      newBoardHash: board.getBoardHash(),
    };
  }

  return {
    success: true,
    capturedStones,
    newBoardHash: newHash,
  };
}

/**
 * Creates a GoBoard from an array of stones.
 */
export function createBoardFromStones(
  size: number,
  stones: Array<{ position: Position; color: StoneColor }>
): GoBoard {
  const board = new GoBoard(size);
  for (const stone of stones) {
    board.setStone(stone.position, stone.color);
  }
  return board;
}

/**
 * Applies a move to a board and returns the new board state.
 * Assumes the move has already been validated.
 */
export function applyMove(
  board: GoBoard,
  position: Position,
  color: StoneColor,
  capturedStones: Position[]
): GoBoard {
  const newBoard = board.clone();
  newBoard.setStone(position, color);
  for (const captured of capturedStones) {
    newBoard.removeStone(captured);
  }
  return newBoard;
}
