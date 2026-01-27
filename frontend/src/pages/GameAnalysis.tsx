import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import gameService from '../services/gameService';
import authService from '../services/authService';
import { Game } from '../types/Game';
import Board from '../components/Game/Board';
import AnalysisControls from '../components/Analysis/AnalysisControls';
import { reconstructBoardAtMove, buildMoveNumberMap, getLastMovePosition } from '../utils/boardReconstruction';
import './GameAnalysis.css';

interface LocationState {
  moveIndex?: number;
}

const GameAnalysis = () => {
  const { gameId } = useParams<{ gameId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const locationState = location.state as LocationState | null;
  const [game, setGame] = useState<Game | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [currentMoveIndex, setCurrentMoveIndex] = useState(-1);
  const [showMoveNumbers, setShowMoveNumbers] = useState(false);
  const [showTerritory, setShowTerritory] = useState(false);
  const currentUsername = authService.getCurrentUsername();

  useEffect(() => {
    const loadGame = async () => {
      if (!gameId) return;

      try {
        const gameData = await gameService.getGame(gameId);
        setGame(gameData);
        // Use moveIndex from location state if provided, otherwise start at the end
        const initialMoveIndex = locationState?.moveIndex ?? gameData.moves.length - 1;
        setCurrentMoveIndex(initialMoveIndex);
        setError('');
      } catch (err: any) {
        setError(err.response?.data?.error || 'Failed to load game');
      } finally {
        setLoading(false);
      }
    };

    loadGame();
  }, [gameId, locationState?.moveIndex]);

  // Keyboard navigation
  useEffect(() => {
    if (!game) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if user is typing in an input
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return;
      }

      switch (e.key) {
        case 'ArrowLeft':
          e.preventDefault();
          setCurrentMoveIndex(prev => Math.max(-1, prev - 1));
          break;
        case 'ArrowRight':
          e.preventDefault();
          setCurrentMoveIndex(prev => Math.min(game.moves.length - 1, prev + 1));
          break;
        case 'Home':
          e.preventDefault();
          setCurrentMoveIndex(-1);
          break;
        case 'End':
          e.preventDefault();
          setCurrentMoveIndex(game.moves.length - 1);
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [game]);

  // When leaving the final position, disable territory display
  useEffect(() => {
    if (game && currentMoveIndex !== game.moves.length - 1) {
      setShowTerritory(false);
    }
  }, [currentMoveIndex, game]);

  const goToMove = useCallback((index: number) => {
    if (!game) return;
    const clampedIndex = Math.max(-1, Math.min(game.moves.length - 1, index));
    setCurrentMoveIndex(clampedIndex);
  }, [game]);

  const handleCreateFork = useCallback(() => {
    if (!gameId) return;
    navigate(`/analyze/${gameId}/fork`, { state: { moveIndex: currentMoveIndex } });
  }, [gameId, currentMoveIndex, navigate]);

  if (loading) {
    return (
      <div className="container">
        <div className="loading">Loading game...</div>
      </div>
    );
  }

  if (error || !game) {
    return (
      <div className="container">
        <div className="error">{error || 'Game not found'}</div>
        <button onClick={() => navigate('/lobby?tab=completed')} className="button button-primary">
          Back to Lobby
        </button>
      </div>
    );
  }

  // Reconstruct board at current position
  const stones = reconstructBoardAtMove(game.moves, currentMoveIndex);
  const moveNumberMap = showMoveNumbers ? buildMoveNumberMap(game.moves, currentMoveIndex) : undefined;
  const lastMovePosition = getLastMovePosition(game.moves, currentMoveIndex);

  // Territory display (only at final position)
  const atFinalPosition = currentMoveIndex === game.moves.length - 1;
  const territory = showTerritory && atFinalPosition && game.result?.territory
    ? game.result.territory
    : undefined;
  const deadStones = showTerritory && atFinalPosition && game.result?.deadStones
    ? game.result.deadStones
    : undefined;

  // Format result text
  const getResultText = () => {
    if (!game.result) return 'Game completed';

    const winner = game.result.winner === 'black' ? game.blackPlayer : game.whitePlayer;
    const isYou = (game.result.winner === 'black' && currentUsername === game.blackPlayer) ||
                  (game.result.winner === 'white' && currentUsername === game.whitePlayer);

    switch (game.result.method) {
      case 'resignation':
        return `${isYou ? 'You' : winner} won by resignation`;
      case 'score':
        if (game.result.score) {
          const margin = Math.abs(game.result.score.black - game.result.score.white);
          return `${isYou ? 'You' : winner} won by ${margin} points`;
        }
        return `${isYou ? 'You' : winner} won`;
      case 'timeout':
        return `${isYou ? 'You' : winner} won on time`;
      default:
        return 'Game completed';
    }
  };

  return (
    <div className="container">
      <div className="analysis-header">
        <button onClick={() => navigate('/lobby?tab=completed')} className="button button-secondary">
          ← Back to Lobby
        </button>
        <h2>Game Analysis</h2>
      </div>

      <div className="analysis-container">
        <div className="analysis-board-section">
          <Board
            size={game.boardSize}
            stones={stones}
            disabled={true}
            lastMove={lastMovePosition}
            territory={territory}
            deadStones={deadStones}
            showMoveNumbers={showMoveNumbers}
            moveNumberMap={moveNumberMap}
          />

          <div className="analysis-controls-section">
            <AnalysisControls
              currentMoveIndex={currentMoveIndex}
              totalMoves={game.moves.length}
              moves={game.moves}
              showMoveNumbers={showMoveNumbers}
              showTerritory={showTerritory}
              onShowMoveNumbersChange={setShowMoveNumbers}
              onShowTerritoryChange={setShowTerritory}
              onGoToMove={goToMove}
              onCreateFork={handleCreateFork}
            />

            <div className="keyboard-hints">
              <h4>Keyboard Shortcuts</h4>
              <div className="shortcuts-list">
                <span><kbd>←</kbd> Previous</span>
                <span><kbd>→</kbd> Next</span>
                <span><kbd>Home</kbd> Start</span>
                <span><kbd>End</kbd> End</span>
              </div>
            </div>
          </div>
        </div>

        <div className="analysis-sidebar">
          <div className="game-info-panel">
            <h3>Game Info</h3>
            <div className="info-row">
              <span className="label">Black:</span>
              <span className="value">
                {game.blackPlayer}
                {game.blackPlayer === currentUsername && ' (you)'}
              </span>
            </div>
            <div className="info-row">
              <span className="label">White:</span>
              <span className="value">
                {game.whitePlayer}
                {game.whitePlayer === currentUsername && ' (you)'}
              </span>
            </div>
            <div className="info-row">
              <span className="label">Board:</span>
              <span className="value">{game.boardSize}×{game.boardSize}</span>
            </div>
            <div className="info-row">
              <span className="label">Komi:</span>
              <span className="value">{game.komi}</span>
            </div>
            <div className="info-row">
              <span className="label">Result:</span>
              <span className="value result">{getResultText()}</span>
            </div>
            {game.result?.score && (
              <div className="info-row">
                <span className="label">Score:</span>
                <span className="value">
                  Black {game.result.score.black} - White {game.result.score.white}
                </span>
              </div>
            )}
            {game.result?.captures && (
              <>
                <div className="info-section-header">Captured Stones</div>
                <div className="info-row">
                  <span className="label">Black:</span>
                  <span className="value">{game.result.captures.black}</span>
                </div>
                <div className="info-row">
                  <span className="label">White:</span>
                  <span className="value">{game.result.captures.white}</span>
                </div>
              </>
            )}
            {game.result?.deadStones && (
              game.result.deadStones.blackDeadStones.length > 0 ||
              game.result.deadStones.whiteDeadStones.length > 0
            ) && (
              <>
                <div className="info-section-header">Prisoners (Dead Stones)</div>
                <div className="info-row">
                  <span className="label">Black:</span>
                  <span className="value">{game.result.deadStones.whiteDeadStones.length}</span>
                </div>
                <div className="info-row">
                  <span className="label">White:</span>
                  <span className="value">{game.result.deadStones.blackDeadStones.length}</span>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default GameAnalysis;
