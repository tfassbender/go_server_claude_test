import { Position } from './Position';

export interface Move {
  player: 'black' | 'white';
  action: 'place' | 'pass' | 'resign';
  position?: Position;
  timestamp: string;
  capturedStones: Position[];
}
