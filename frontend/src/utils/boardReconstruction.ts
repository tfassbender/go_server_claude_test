import { Move } from '../types/Move';
import { StoneOnBoard } from '../types/Game';

/**
 * Reconstructs the board state at a specific move index.
 * @param moves - Array of all moves in the game
 * @param moveIndex - The index to reconstruct to (inclusive, -1 means empty board)
 * @returns Array of stones on the board at that position
 */
export function reconstructBoardAtMove(moves: Move[], moveIndex: number): StoneOnBoard[] {
  const board = new Map<string, 'black' | 'white'>();

  for (let i = 0; i <= moveIndex && i < moves.length; i++) {
    const move = moves[i];
    if (move.action === 'place' && move.position) {
      const key = `${move.position.x},${move.position.y}`;
      board.set(key, move.player);

      // Remove captured stones
      move.capturedStones.forEach(pos => {
        board.delete(`${pos.x},${pos.y}`);
      });
    }
    // Pass and resign moves don't change the board
  }

  const stones: StoneOnBoard[] = [];
  board.forEach((color, key) => {
    const [x, y] = key.split(',').map(Number);
    stones.push({ position: { x, y }, color });
  });

  return stones;
}

/**
 * Builds a map from position key to move number for stones currently on the board.
 * Only includes stones that are still on the board at the given move index.
 * @param moves - Array of all moves in the game
 * @param moveIndex - The index to build the map for (inclusive)
 * @returns Map from "x,y" to 1-based move number
 */
export function buildMoveNumberMap(moves: Move[], moveIndex: number): Map<string, number> {
  // Track which positions have stones and when they were placed
  const stonePlacement = new Map<string, number>(); // position -> move number (1-based)
  const removedStones = new Set<string>();

  for (let i = 0; i <= moveIndex && i < moves.length; i++) {
    const move = moves[i];
    if (move.action === 'place' && move.position) {
      const key = `${move.position.x},${move.position.y}`;
      stonePlacement.set(key, i + 1); // 1-based move number
      removedStones.delete(key); // Stone is back if it was captured before

      // Mark captured stones as removed
      move.capturedStones.forEach(pos => {
        const capturedKey = `${pos.x},${pos.y}`;
        removedStones.add(capturedKey);
      });
    }
  }

  // Filter out stones that have been captured
  const result = new Map<string, number>();
  stonePlacement.forEach((moveNumber, key) => {
    if (!removedStones.has(key)) {
      result.set(key, moveNumber);
    }
  });

  return result;
}

/**
 * Finds the position of the last move that was a stone placement.
 * @param moves - Array of all moves in the game
 * @param moveIndex - The current move index
 * @returns The position of the last placed stone, or undefined
 */
export function getLastMovePosition(moves: Move[], moveIndex: number): { x: number; y: number } | undefined {
  if (moveIndex < 0 || moveIndex >= moves.length) {
    return undefined;
  }

  // Look backwards from current move to find the last placement
  for (let i = moveIndex; i >= 0; i--) {
    const move = moves[i];
    if (move.action === 'place' && move.position) {
      return move.position;
    }
  }

  return undefined;
}
