import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import gameService from '../services/gameService';
import authService from '../services/authService';
import { useGameEvents } from '../hooks/useGameEvents';
import { Game } from '../types/Game';
import { Position } from '../types/Position';
import Board from '../components/Game/Board';
import GameInfo from '../components/Game/GameInfo';
import GameControls from '../components/Game/GameControls';
import './GamePlay.css';

const GamePlay = () => {
  const { gameId } = useParams<{ gameId: string }>();
  const navigate = useNavigate();
  const [game, setGame] = useState<Game | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [moveError, setMoveError] = useState('');
  const [sseConnected, setSseConnected] = useState(false);
  const currentUsername = authService.getCurrentUsername();

  const loadGame = async () => {
    if (!gameId) return;

    try {
      const gameData = await gameService.getGame(gameId);
      setGame(gameData);
      setError('');
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to load game');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGame();
  }, [gameId]);

  const handleGameEvent = useCallback((event: any) => {
    console.log('Received game event:', event);

    if (event.type === 'connected') {
      setSseConnected(true);
      return;
    }

    if (event.type === 'disconnected') {
      setSseConnected(false);
      return;
    }

    // Reload game on any event (move, pass, gameEnd)
    loadGame();
  }, [gameId]);

  useGameEvents(gameId, handleGameEvent);

  const handleIntersectionClick = async (position: Position) => {
    if (!game || !gameId) return;

    // Check if it's player's turn
    const yourColor = currentUsername === game.blackPlayer ? 'black' : 'white';
    if (game.currentTurn !== yourColor) {
      setMoveError('It\'s not your turn');
      return;
    }

    if (game.status !== 'active') {
      setMoveError('Game is not active');
      return;
    }

    try {
      setMoveError('');
      const response = await gameService.makeMove(gameId, position);

      if (response.success) {
        // Reload game to show the new move
        await loadGame();
      } else {
        setMoveError(response.error || 'Invalid move');
      }
    } catch (err: any) {
      setMoveError(err.response?.data?.error || 'Failed to make move');
    }
  };

  const handlePass = async () => {
    if (!gameId) return;

    try {
      setMoveError('');
      await gameService.pass(gameId);
      await loadGame();
    } catch (err: any) {
      setMoveError(err.response?.data?.error || 'Failed to pass');
    }
  };

  const handleResign = async () => {
    if (!gameId) return;

    try {
      setMoveError('');
      await gameService.resign(gameId);
      await loadGame();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to resign');
    }
  };

  if (loading) {
    return (
      <div className="container">
        <div className="loading">Loading game...</div>
      </div>
    );
  }

  if (error || !game || !currentUsername) {
    return (
      <div className="container">
        <div className="error">{error || 'Game not found'}</div>
        <button onClick={() => navigate('/lobby')} className="button button-primary">
          Back to Lobby
        </button>
      </div>
    );
  }

  const yourColor = currentUsername === game.blackPlayer ? 'black' : 'white';
  const isYourTurn = game.currentTurn === yourColor && game.status === 'active';
  const lastMove = game.moves.length > 0 ? game.moves[game.moves.length - 1].position : undefined;

  return (
    <div className="container">
      <div className="game-header">
        <button onClick={() => navigate('/lobby')} className="button button-secondary">
          ← Back to Lobby
        </button>
        <div className="connection-status">
          {sseConnected ? (
            <span className="connected">🟢 Live updates enabled</span>
          ) : (
            <span className="disconnected">⚪ Connecting...</span>
          )}
        </div>
      </div>

      <div className="gameplay-container">
        <div className="board-section">
          <Board
            size={game.boardSize}
            stones={game.boardState?.stones || []}
            onIntersectionClick={handleIntersectionClick}
            disabled={!isYourTurn}
            lastMove={lastMove}
          />
          {moveError && <div className="error move-error">{moveError}</div>}
        </div>

        <div className="sidebar">
          <GameInfo game={game} currentUsername={currentUsername} />
          {game.status === 'active' && (
            <GameControls
              onPass={handlePass}
              onResign={handleResign}
              disabled={!isYourTurn}
            />
          )}
        </div>
      </div>
    </div>
  );
};

export default GamePlay;
