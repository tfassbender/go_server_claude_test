import apiClient from './apiClient';
import { Game, GameListItem, GameResult } from '../types/Game';
import { Position } from '../types/Position';
import { Move } from '../types/Move';

export interface CreateGameRequest {
  boardSize: number;
  opponentUsername: string;
  requestedColor: 'black' | 'white' | 'random';
  komi: number;
}

export interface MoveRequest {
  action: 'place';
  position: Position;
}

export interface MoveResponse {
  success: boolean;
  error?: string;
  capturedStones?: Position[];
  currentTurn?: 'black' | 'white';
}

const gameService = {
  async createGame(request: CreateGameRequest): Promise<Game> {
    const response = await apiClient.post<Game>('/games', request);
    return response.data;
  },

  async getGames(status?: 'pending' | 'active' | 'completed'): Promise<GameListItem[]> {
    const params = status ? { status } : {};
    const response = await apiClient.get<GameListItem[]>('/games', { params });
    return response.data;
  },

  async getGame(gameId: string): Promise<Game> {
    const response = await apiClient.get<Game>(`/games/${gameId}`);
    return response.data;
  },

  async acceptGame(gameId: string): Promise<void> {
    await apiClient.post(`/games/${gameId}/accept`);
  },

  async declineGame(gameId: string): Promise<void> {
    await apiClient.post(`/games/${gameId}/decline`);
  },

  async makeMove(gameId: string, position: Position): Promise<MoveResponse> {
    const response = await apiClient.post<MoveResponse>(`/games/${gameId}/move`, {
      action: 'place',
      position
    });
    return response.data;
  },

  async pass(gameId: string): Promise<void> {
    await apiClient.post(`/games/${gameId}/pass`);
  },

  async resign(gameId: string): Promise<void> {
    await apiClient.post(`/games/${gameId}/resign`);
  },

  async recalculateScore(gameId: string, manuallyMarkedDeadStones: Position[]): Promise<GameResult> {
    const response = await apiClient.post<GameResult>(
      `/games/${gameId}/recalculate-score`,
      { manuallyMarkedDeadStones }
    );
    return response.data;
  },

  async calculateForkScore(
    boardSize: number,
    moves: Move[],
    komi: number,
    manuallyMarkedDeadStones: Position[]
  ): Promise<GameResult> {
    const response = await apiClient.post<GameResult>(
      '/games/calculate-fork-score',
      { boardSize, moves, komi, manuallyMarkedDeadStones }
    );
    return response.data;
  }
};

export default gameService;
