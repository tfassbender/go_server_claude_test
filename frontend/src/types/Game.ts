import { Move } from './Move';
import { Position } from './Position';

export interface Game {
  id: string;
  boardSize: number;
  blackPlayer: string;
  whitePlayer: string;
  currentTurn: 'black' | 'white';
  status: 'pending' | 'active' | 'completed';
  createdAt: string;
  lastMoveAt: string;
  moves: Move[];
  passes: number;
  result?: GameResult;
  boardState?: BoardState;
}

export interface GameResult {
  winner: 'black' | 'white';
  method: 'resignation' | 'score' | 'timeout';
  score?: {
    black: number;
    white: number;
  };
}

export interface BoardState {
  stones: StoneOnBoard[];
}

export interface StoneOnBoard {
  position: Position;
  color: 'black' | 'white';
}

export interface GameListItem {
  id: string;
  opponent: string;
  yourColor: 'black' | 'white';
  currentTurn: 'black' | 'white';
  lastMoveAt: string;
  status: 'pending' | 'active' | 'completed';
  boardSize: number;
  isCreator: boolean;
}
