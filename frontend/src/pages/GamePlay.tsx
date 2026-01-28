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
import FixTerritoryControls from '../components/Game/FixTerritoryControls';
import './GamePlay.css';

// Detect if the device is mobile
const isMobileDevice = (): boolean => {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ||
    (window.innerWidth <= 768);
};

const GamePlay = () => {
  const { gameId } = useParams<{ gameId: string }>();
  const navigate = useNavigate();
  const [game, setGame] = useState<Game | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [moveError, setMoveError] = useState('');
  const [sseConnected, setSseConnected] = useState(false);
  const [fixMode, setFixMode] = useState(false);
  const [markedDeadStones, setMarkedDeadStones] = useState<Position[]>([]);
  const [recalculateLoading, setRecalculateLoading] = useState(false);
  const [recalculateError, setRecalculateError] = useState('');
  const [pendingMove, setPendingMove] = useState<Position | null>(null);
  const [autoSubmit, setAutoSubmit] = useState<boolean>(() => !isMobileDevice());
  const [submitting, setSubmitting] = useState(false);
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

    setMoveError('');

    if (autoSubmit) {
      // Auto-submit mode: submit immediately
      try {
        const response = await gameService.makeMove(gameId, position);

        if (response.success) {
          await loadGame();
        } else {
          setMoveError(response.error || 'Invalid move');
        }
      } catch (err: any) {
        setMoveError(err.response?.data?.error || 'Failed to make move');
      }
    } else {
      // Manual submit mode: set pending move (or update if clicking different position)
      if (pendingMove && pendingMove.x === position.x && pendingMove.y === position.y) {
        // Clicking the same position cancels the pending move
        setPendingMove(null);
      } else {
        setPendingMove(position);
      }
    }
  };

  const handleSubmitMove = async () => {
    if (!game || !gameId || !pendingMove) return;

    try {
      setSubmitting(true);
      setMoveError('');
      const response = await gameService.makeMove(gameId, pendingMove);

      if (response.success) {
        setPendingMove(null);
        await loadGame();
      } else {
        setMoveError(response.error || 'Invalid move');
      }
    } catch (err: any) {
      setMoveError(err.response?.data?.error || 'Failed to make move');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelPendingMove = () => {
    setPendingMove(null);
    setMoveError('');
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

  const toggleFixMode = () => {
    setFixMode(!fixMode);
    setMarkedDeadStones([]);
    setRecalculateError('');
  };

  const toggleDeadStone = (position: Position) => {
    setMarkedDeadStones(prev => {
      const exists = prev.some(p => p.x === position.x && p.y === position.y);
      if (exists) {
        return prev.filter(p => !(p.x === position.x && p.y === position.y));
      } else {
        return [...prev, position];
      }
    });
  };

  const handleRecalculate = async () => {
    if (!gameId) return;

    try {
      setRecalculateLoading(true);
      setRecalculateError('');
      await gameService.recalculateScore(gameId, markedDeadStones);
      await loadGame();
      setFixMode(false);
      setMarkedDeadStones([]);
    } catch (err: any) {
      setRecalculateError(err.response?.data?.error || 'Failed to recalculate score');
    } finally {
      setRecalculateLoading(false);
    }
  };

  const clearMarkedStones = () => {
    setMarkedDeadStones([]);
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

  const lobbyUrl = game.status === 'completed' ? '/lobby?tab=completed' : '/lobby';

  const yourColor = currentUsername === game.blackPlayer ? 'black' : 'white';
  const isYourTurn = game.currentTurn === yourColor && game.status === 'active';
  const lastMove = game.moves.length > 0 ? game.moves[game.moves.length - 1].position : undefined;

  return (
    <div className="container">
      <div className="game-header">
        <button onClick={() => navigate(lobbyUrl)} className="button button-secondary">
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
            disabled={!isYourTurn || fixMode || submitting}
            lastMove={lastMove}
            territory={game.status === 'completed' && game.result?.territory && !fixMode ? game.result.territory : undefined}
            fixMode={fixMode}
            markedDeadStones={markedDeadStones}
            onStoneClick={toggleDeadStone}
            deadStones={game.status === 'completed' && game.result?.deadStones && !fixMode ? game.result.deadStones : undefined}
            pendingMove={pendingMove || undefined}
            pendingMoveColor={yourColor}
          />

          {/* Submit Controls - Below Board */}
          {game.status === 'active' && (
            <div className="submit-controls">
              <div className="submit-button-row">
                <button
                  className={`button button-primary submit-move-button${autoSubmit ? ' disabled-auto' : ''}`}
                  onClick={handleSubmitMove}
                  disabled={autoSubmit || !pendingMove || submitting || !isYourTurn}
                >
                  {submitting ? 'Submitting...' : 'Submit Move'}
                </button>
                {pendingMove && !autoSubmit && (
                  <button
                    className="button button-secondary cancel-move-button"
                    onClick={handleCancelPendingMove}
                    disabled={submitting}
                  >
                    Cancel
                  </button>
                )}
              </div>
              <label className="auto-submit-checkbox">
                <input
                  type="checkbox"
                  checked={autoSubmit}
                  onChange={(e) => {
                    setAutoSubmit(e.target.checked);
                    if (e.target.checked) {
                      setPendingMove(null);
                    }
                  }}
                />
                Auto-submit moves
              </label>
            </div>
          )}

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
          {game.status === 'completed' && game.result?.method === 'score' && (
            <FixTerritoryControls
              fixMode={fixMode}
              markedCount={markedDeadStones.length}
              onToggleFixMode={toggleFixMode}
              onRecalculate={handleRecalculate}
              onClear={clearMarkedStones}
              loading={recalculateLoading}
              error={recalculateError}
            />
          )}
        </div>
      </div>
    </div>
  );
};

export default GamePlay;
