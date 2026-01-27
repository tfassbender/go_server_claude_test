import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import gameService from '../services/gameService';
import { Game, StoneOnBoard } from '../types/Game';
import { Position } from '../types/Position';
import Board from '../components/Game/Board';
import ForkControls from '../components/Analysis/ForkControls';
import { reconstructBoardAtMove, buildMoveNumberMap } from '../utils/boardReconstruction';
import {
  GoBoard,
  StoneColor,
  validateAndExecuteMove,
  createBoardFromStones,
} from '../utils/goRulesEngine';
import './ForkAnalysis.css';

interface ForkMove {
  position: Position | null; // null for pass moves
  color: StoneColor;
  capturedStones: Position[];
  boardHashBefore: string; // Used for Ko detection
}

interface LocationState {
  moveIndex?: number;
}

const ForkAnalysis = () => {
  const { gameId } = useParams<{ gameId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const locationState = location.state as LocationState | null;

  const [game, setGame] = useState<Game | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [baseMoveIndex, setBaseMoveIndex] = useState<number>(-1);
  const [forkMoves, setForkMoves] = useState<ForkMove[]>([]);
  const [currentForkMoveIndex, setCurrentForkMoveIndex] = useState<number>(-1);
  const [moveError, setMoveError] = useState<string>('');

  // Load the game
  useEffect(() => {
    const loadGame = async () => {
      if (!gameId) return;

      try {
        const gameData = await gameService.getGame(gameId);
        setGame(gameData);

        // Use moveIndex from location state, or default to end of game
        const initialMoveIndex = locationState?.moveIndex ?? gameData.moves.length - 1;
        setBaseMoveIndex(initialMoveIndex);
        setError('');
      } catch (err: any) {
        setError(err.response?.data?.error || 'Failed to load game');
      } finally {
        setLoading(false);
      }
    };

    loadGame();
  }, [gameId, locationState?.moveIndex]);

  // Derive whose turn it is next
  const nextTurn = useMemo((): StoneColor => {
    if (!game) return 'black';

    // Determine the turn after the base position
    let turn: StoneColor;
    if (baseMoveIndex < 0) {
      // Starting from empty board, black goes first
      turn = 'black';
    } else {
      // After the last base move, it's the opposite color's turn
      const lastBaseMove = game.moves[baseMoveIndex];
      turn = lastBaseMove.player === 'black' ? 'white' : 'black';
    }

    // Apply fork moves up to current index to determine next turn
    for (let i = 0; i <= currentForkMoveIndex && i < forkMoves.length; i++) {
      turn = forkMoves[i].color === 'black' ? 'white' : 'black';
    }

    return turn;
  }, [game, baseMoveIndex, forkMoves, currentForkMoveIndex]);

  // Reconstruct the base board state
  const baseStones = useMemo((): StoneOnBoard[] => {
    if (!game) return [];
    return reconstructBoardAtMove(game.moves, baseMoveIndex);
  }, [game, baseMoveIndex]);

  // Build the current board state (base + fork moves)
  const currentBoard = useMemo((): GoBoard => {
    if (!game) return new GoBoard(19);

    const board = createBoardFromStones(game.boardSize, baseStones);

    // Apply fork moves up to current index
    for (let i = 0; i <= currentForkMoveIndex && i < forkMoves.length; i++) {
      const forkMove = forkMoves[i];
      if (forkMove.position) {
        board.setStone(forkMove.position, forkMove.color);
        for (const captured of forkMove.capturedStones) {
          board.removeStone(captured);
        }
      }
      // Pass moves don't modify the board
    }

    return board;
  }, [game, baseStones, forkMoves, currentForkMoveIndex]);

  // Convert board to stones array for Board component
  const currentStones = useMemo((): StoneOnBoard[] => {
    return currentBoard.getAllStones();
  }, [currentBoard]);

  // Build move number map for display
  const moveNumberMap = useMemo((): Map<string, number> => {
    const map = new Map<string, number>();

    // Base moves (stones still on board)
    if (game) {
      const baseMoveMap = buildMoveNumberMap(game.moves, baseMoveIndex);
      baseMoveMap.forEach((num, key) => {
        // Only include if stone is still on board after fork moves
        if (currentBoard.getStone({ x: parseInt(key.split(',')[0]), y: parseInt(key.split(',')[1]) })) {
          map.set(key, num);
        }
      });
    }

    // Fork moves (numbered starting from base + 1)
    const baseCount = game ? Math.min(baseMoveIndex + 1, game.moves.length) : 0;
    for (let i = 0; i <= currentForkMoveIndex && i < forkMoves.length; i++) {
      const forkMove = forkMoves[i];
      if (forkMove.position) {
        const key = `${forkMove.position.x},${forkMove.position.y}`;
        // Only show number if stone is still on board
        if (currentBoard.getStone(forkMove.position)) {
          map.set(key, baseCount + i + 1);
        }
      }
    }

    return map;
  }, [game, baseMoveIndex, forkMoves, currentForkMoveIndex, currentBoard]);

  // Get last move position for highlighting
  const lastMovePosition = useMemo((): Position | undefined => {
    // Check fork moves first (from current index backwards)
    for (let i = currentForkMoveIndex; i >= 0; i--) {
      if (forkMoves[i]?.position) {
        return forkMoves[i].position!;
      }
    }

    // Fall back to base game last move
    if (game && baseMoveIndex >= 0) {
      for (let i = baseMoveIndex; i >= 0; i--) {
        if (game.moves[i].action === 'place' && game.moves[i].position) {
          return game.moves[i].position;
        }
      }
    }

    return undefined;
  }, [game, baseMoveIndex, forkMoves, currentForkMoveIndex]);

  // Get Ko blocked hash (board state before the last capturing move)
  const koBlockedHash = useMemo((): string | null => {
    // Ko: if last move captured exactly one stone, the board hash before that move is blocked
    const lastMoveIdx = currentForkMoveIndex;
    if (lastMoveIdx >= 0 && lastMoveIdx < forkMoves.length) {
      const lastMove = forkMoves[lastMoveIdx];
      if (lastMove.capturedStones.length === 1) {
        return lastMove.boardHashBefore;
      }
    }
    return null;
  }, [forkMoves, currentForkMoveIndex]);

  // Handle placing a stone
  const handleIntersectionClick = useCallback((position: Position) => {
    if (!game) return;

    setMoveError('');

    const result = validateAndExecuteMove(currentBoard, position, nextTurn, koBlockedHash);

    if (!result.success) {
      setMoveError(result.error || 'Invalid move');
      return;
    }

    const newMove: ForkMove = {
      position,
      color: nextTurn,
      capturedStones: result.capturedStones,
      boardHashBefore: currentBoard.getBoardHash(),
    };

    // If we're not at the end of fork history, truncate future moves
    const newForkMoves = forkMoves.slice(0, currentForkMoveIndex + 1);
    newForkMoves.push(newMove);

    setForkMoves(newForkMoves);
    setCurrentForkMoveIndex(newForkMoves.length - 1);
  }, [game, currentBoard, nextTurn, koBlockedHash, forkMoves, currentForkMoveIndex]);

  // Handle pass
  const handlePass = useCallback(() => {
    if (!game) return;

    setMoveError('');

    const newMove: ForkMove = {
      position: null,
      color: nextTurn,
      capturedStones: [],
      boardHashBefore: currentBoard.getBoardHash(),
    };

    const newForkMoves = forkMoves.slice(0, currentForkMoveIndex + 1);
    newForkMoves.push(newMove);

    setForkMoves(newForkMoves);
    setCurrentForkMoveIndex(newForkMoves.length - 1);
  }, [game, nextTurn, currentBoard, forkMoves, currentForkMoveIndex]);

  // Handle undo
  const handleUndo = useCallback(() => {
    if (currentForkMoveIndex >= 0) {
      setCurrentForkMoveIndex(currentForkMoveIndex - 1);
      setMoveError('');
    }
  }, [currentForkMoveIndex]);

  // Handle redo
  const handleRedo = useCallback(() => {
    if (currentForkMoveIndex < forkMoves.length - 1) {
      setCurrentForkMoveIndex(currentForkMoveIndex + 1);
      setMoveError('');
    }
  }, [currentForkMoveIndex, forkMoves.length]);

  // Handle clear all
  const handleClearAll = useCallback(() => {
    setForkMoves([]);
    setCurrentForkMoveIndex(-1);
    setMoveError('');
  }, []);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if user is typing in an input
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return;
      }

      if (e.ctrlKey || e.metaKey) {
        if (e.key === 'z' || e.key === 'Z') {
          e.preventDefault();
          handleUndo();
        } else if (e.key === 'y' || e.key === 'Y') {
          e.preventDefault();
          handleRedo();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleUndo, handleRedo]);

  // Navigation
  const handleBackToAnalysis = () => {
    navigate(`/analyze/${gameId}`);
  };

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

  const canUndo = currentForkMoveIndex >= 0;
  const canRedo = currentForkMoveIndex < forkMoves.length - 1;

  // Display base move number (1-based)
  const displayBaseMoveNumber = baseMoveIndex + 1;

  return (
    <div className="container">
      <div className="fork-header">
        <button onClick={handleBackToAnalysis} className="button button-secondary">
          Back to Analysis
        </button>
        <div className="fork-mode-banner">
          <span className="fork-label">FORK MODE</span>
          <span className="fork-info">
            {baseMoveIndex < 0
              ? '(forked from start)'
              : `(forked from move ${displayBaseMoveNumber})`}
          </span>
        </div>
      </div>

      {moveError && (
        <div className="move-error">
          {moveError}
        </div>
      )}

      <div className="fork-container">
        <div className="fork-board-section">
          <Board
            size={game.boardSize}
            stones={currentStones}
            disabled={false}
            onIntersectionClick={handleIntersectionClick}
            lastMove={lastMovePosition}
            showMoveNumbers={true}
            moveNumberMap={moveNumberMap}
          />
        </div>

        <div className="fork-sidebar">
          <ForkControls
            nextTurn={nextTurn}
            forkMoveCount={forkMoves.length}
            currentForkMoveIndex={currentForkMoveIndex}
            canUndo={canUndo}
            canRedo={canRedo}
            onUndo={handleUndo}
            onRedo={handleRedo}
            onClearAll={handleClearAll}
            onPass={handlePass}
          />

          <div className="fork-info-panel">
            <h3>Fork Info</h3>
            <div className="info-row">
              <span className="label">Base Game:</span>
              <span className="value">{game.blackPlayer} vs {game.whitePlayer}</span>
            </div>
            <div className="info-row">
              <span className="label">Board:</span>
              <span className="value">{game.boardSize}x{game.boardSize}</span>
            </div>
            <div className="info-row">
              <span className="label">Forked at:</span>
              <span className="value">
                {baseMoveIndex < 0 ? 'Start (empty board)' : `Move ${displayBaseMoveNumber}`}
              </span>
            </div>
            <div className="info-row">
              <span className="label">Fork moves:</span>
              <span className="value">{forkMoves.length}</span>
            </div>
          </div>

          <div className="fork-notice">
            Fork moves are temporary and will be lost when you leave this page.
          </div>
        </div>
      </div>
    </div>
  );
};

export default ForkAnalysis;
