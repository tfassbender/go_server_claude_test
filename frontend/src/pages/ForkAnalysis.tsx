import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import gameService from '../services/gameService';
import { Game, GameResult, StoneOnBoard } from '../types/Game';
import { Move } from '../types/Move';
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

  // Display options
  const [showMoveNumbers, setShowMoveNumbers] = useState<boolean>(true);

  // AI move suggestion state
  const [aiLevel, setAiLevel] = useState<number>(10); // Default to maximum strength
  const [aiMoveLoading, setAiMoveLoading] = useState<boolean>(false);

  // Score calculation state
  const [scoreResult, setScoreResult] = useState<GameResult | null>(null);
  const [fixMode, setFixMode] = useState<boolean>(false);
  const [markedDeadStones, setMarkedDeadStones] = useState<Position[]>([]);
  const [calculatingScore, setCalculatingScore] = useState<boolean>(false);
  const [scoreError, setScoreError] = useState<string>('');

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

  // Calculate consecutive passes
  const consecutivePasses = useMemo((): number => {
    let passes = 0;
    // Count backwards from current fork move index
    for (let i = currentForkMoveIndex; i >= 0 && i < forkMoves.length; i--) {
      const move = forkMoves[i];
      if (move.position === null) {
        passes++;
      } else {
        break; // Stop at first non-pass move
      }
    }
    return passes;
  }, [forkMoves, currentForkMoveIndex]);

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
    if (!game || fixMode || consecutivePasses >= 2 || aiMoveLoading) return;

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

    // Reset score when making a new move
    if (scoreResult) {
      setScoreResult(null);
      setScoreError('');
    }
  }, [game, fixMode, consecutivePasses, aiMoveLoading, currentBoard, nextTurn, koBlockedHash, forkMoves, currentForkMoveIndex, scoreResult]);

  // Handle pass
  const handlePass = useCallback(() => {
    if (!game || fixMode || consecutivePasses >= 2 || aiMoveLoading) return;

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

    // Reset score when making a new move
    if (scoreResult) {
      setScoreResult(null);
      setScoreError('');
    }
  }, [game, fixMode, consecutivePasses, aiMoveLoading, nextTurn, currentBoard, forkMoves, currentForkMoveIndex, scoreResult]);

  // Handle get AI move
  const handleGetAiMove = useCallback(async () => {
    if (!game || fixMode || aiMoveLoading || consecutivePasses >= 2) return;

    setMoveError('');
    setAiMoveLoading(true);

    try {
      // Build the combined moves (base + fork) to send to the AI
      const combinedMoves = buildCombinedMoves();

      // Request AI move suggestion
      const response = await gameService.getAiMoveSuggestion(
        game.boardSize,
        combinedMoves,
        game.komi,
        aiLevel,
        nextTurn
      );

      if (!response.success) {
        setMoveError('Failed to get AI move suggestion');
        return;
      }

      // Handle pass move
      if (response.isPass || response.position === null) {
        handlePass();
        return;
      }

      // Validate and execute the AI's suggested move
      const result = validateAndExecuteMove(currentBoard, response.position, nextTurn, koBlockedHash);

      if (!result.success) {
        setMoveError(result.error || 'AI suggested an invalid move');
        return;
      }

      const newMove: ForkMove = {
        position: response.position,
        color: nextTurn,
        capturedStones: result.capturedStones,
        boardHashBefore: currentBoard.getBoardHash(),
      };

      // If we're not at the end of fork history, truncate future moves
      const newForkMoves = forkMoves.slice(0, currentForkMoveIndex + 1);
      newForkMoves.push(newMove);

      setForkMoves(newForkMoves);
      setCurrentForkMoveIndex(newForkMoves.length - 1);

      // Reset score when making a new move
      if (scoreResult) {
        setScoreResult(null);
        setScoreError('');
      }
    } catch (err: any) {
      setMoveError(err.response?.data?.error || 'Failed to get AI move suggestion');
    } finally {
      setAiMoveLoading(false);
    }
  }, [game, fixMode, aiMoveLoading, consecutivePasses, aiLevel, nextTurn, currentBoard, koBlockedHash, forkMoves, currentForkMoveIndex, scoreResult, handlePass]);

  // Handle undo
  const handleUndo = useCallback(() => {
    if (currentForkMoveIndex >= 0) {
      setCurrentForkMoveIndex(currentForkMoveIndex - 1);
      setMoveError('');
      // Reset score when undoing
      if (scoreResult) {
        setScoreResult(null);
        setFixMode(false);
        setMarkedDeadStones([]);
        setScoreError('');
      }
    }
  }, [currentForkMoveIndex, scoreResult]);

  // Handle redo
  const handleRedo = useCallback(() => {
    if (currentForkMoveIndex < forkMoves.length - 1) {
      setCurrentForkMoveIndex(currentForkMoveIndex + 1);
      setMoveError('');
      // Reset score when redoing
      if (scoreResult) {
        setScoreResult(null);
        setFixMode(false);
        setMarkedDeadStones([]);
        setScoreError('');
      }
    }
  }, [currentForkMoveIndex, forkMoves.length, scoreResult]);

  // Handle clear all
  const handleClearAll = useCallback(() => {
    setForkMoves([]);
    setCurrentForkMoveIndex(-1);
    setMoveError('');
    // Reset score when clearing fork
    setScoreResult(null);
    setFixMode(false);
    setMarkedDeadStones([]);
    setScoreError('');
  }, []);

  // Reset score calculation
  const resetScore = useCallback(() => {
    setScoreResult(null);
    setFixMode(false);
    setMarkedDeadStones([]);
    setScoreError('');
  }, []);

  // Toggle fix territory mode
  const toggleFixMode = useCallback(() => {
    if (fixMode) {
      // Exiting fix mode without calculating
      setFixMode(false);
      setMarkedDeadStones([]);
      setScoreError('');
    } else {
      // Entering fix mode
      setFixMode(true);
      setScoreResult(null);
      setScoreError('');
    }
  }, [fixMode]);

  // Toggle a stone as dead
  const toggleDeadStone = useCallback((position: Position) => {
    setMarkedDeadStones(prev => {
      const exists = prev.some(p => p.x === position.x && p.y === position.y);
      if (exists) {
        return prev.filter(p => p.x !== position.x || p.y !== position.y);
      } else {
        return [...prev, position];
      }
    });
  }, []);

  // Clear marked dead stones
  const clearMarkedDeadStones = useCallback(() => {
    setMarkedDeadStones([]);
  }, []);

  // Build the combined moves (base game + fork moves) for score calculation
  const buildCombinedMoves = useCallback((): Move[] => {
    if (!game) return [];

    // Get base moves up to baseMoveIndex
    const baseMoves = baseMoveIndex >= 0 ? game.moves.slice(0, baseMoveIndex + 1) : [];

    // Convert fork moves to Move format
    const forkMovesConverted: Move[] = forkMoves.slice(0, currentForkMoveIndex + 1).map(fm => ({
      player: fm.color,
      action: fm.position ? 'place' : 'pass',
      position: fm.position || undefined,
      timestamp: new Date().toISOString(),
      capturedStones: fm.capturedStones
    } as Move));

    return [...baseMoves, ...forkMovesConverted];
  }, [game, baseMoveIndex, forkMoves, currentForkMoveIndex]);

  // Calculate score
  const handleCalculateScore = useCallback(async () => {
    if (!game) return;

    setCalculatingScore(true);
    setScoreError('');

    try {
      const combinedMoves = buildCombinedMoves();
      const result = await gameService.calculateForkScore(
        game.boardSize,
        combinedMoves,
        game.komi,
        markedDeadStones
      );
      setScoreResult(result);
      setFixMode(false);
    } catch (err: any) {
      setScoreError(err.response?.data?.error || 'Failed to calculate score');
    } finally {
      setCalculatingScore(false);
    }
  }, [game, buildCombinedMoves, markedDeadStones]);

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
    navigate(`/analyze/${gameId}`, { state: { moveIndex: baseMoveIndex } });
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
            disabled={fixMode || consecutivePasses >= 2 || aiMoveLoading}
            onIntersectionClick={handleIntersectionClick}
            lastMove={lastMovePosition}
            showMoveNumbers={showMoveNumbers && !fixMode && !scoreResult}
            moveNumberMap={moveNumberMap}
            fixMode={fixMode}
            markedDeadStones={markedDeadStones}
            onStoneClick={toggleDeadStone}
            territory={scoreResult?.territory}
            deadStones={scoreResult?.deadStones}
          />

          {/* Score Comparison Section - Below Board */}
          <div className="score-comparison-row">
            {/* Original Game Result - Left Column */}
            <div className="score-column">
              {game.result && game.result.method === 'score' && (
                <div className="original-game-result-panel">
                  <h3>Original Game Result</h3>
                  <div className="winner-announcement">
                    {(() => {
                      const winnerName = game.result.winner === 'black' ? game.blackPlayer : game.whitePlayer;
                      const winnerColor = game.result.winner === 'black' ? 'Black' : 'White';
                      if (game.result.score) {
                        const margin = Math.abs(game.result.score.black - game.result.score.white);
                        return `${winnerColor} (${winnerName}) won by ${margin} points`;
                      }
                      return `${winnerColor} (${winnerName}) won`;
                    })()}
                  </div>
                  {game.result.score && (
                    <div className="score-breakdown">
                      <h4>Final Score</h4>
                      <div className="score-row">
                        <span className="score-label">Black ({game.blackPlayer}):</span>
                        <span className="score-value">{game.result.score.black}</span>
                      </div>
                      <div className="score-row">
                        <span className="score-label">White ({game.whitePlayer}):</span>
                        <span className="score-value">{game.result.score.white}</span>
                      </div>
                      <p className="score-note">(includes {game.komi} komi for white)</p>
                    </div>
                  )}
                </div>
              )}

              {/* Calculate Score Button - Below Original Result */}
              {!scoreResult && !fixMode && (
                <div className="score-calculation-panel">
                  <button
                    onClick={toggleFixMode}
                    className="button button-primary calculate-score-button"
                    title="Calculate the final score for this fork position"
                  >
                    Calculate Score
                  </button>
                </div>
              )}
            </div>

            {/* Fork Score Result - Right Column */}
            {scoreResult && (
              <div className="score-column">
                <div className="score-result-panel">
                  <h3>Fork Score Result</h3>
                  <div className="winner-announcement">
                    {(() => {
                      const winnerName = scoreResult.winner === 'black' ? game.blackPlayer : game.whitePlayer;
                      const winnerColor = scoreResult.winner === 'black' ? 'Black' : 'White';
                      if (scoreResult.score) {
                        const margin = Math.abs(scoreResult.score.black - scoreResult.score.white);
                        return `${winnerColor} (${winnerName}) wins by ${margin} points`;
                      }
                      return `${winnerColor} (${winnerName}) wins`;
                    })()}
                  </div>
                  {scoreResult.score && (
                    <div className="score-breakdown">
                      <h4>Final Score</h4>
                      <div className="score-row">
                        <span className="score-label">Black ({game.blackPlayer}):</span>
                        <span className="score-value">{scoreResult.score.black}</span>
                      </div>
                      <div className="score-row">
                        <span className="score-label">White ({game.whitePlayer}):</span>
                        <span className="score-value">{scoreResult.score.white}</span>
                      </div>
                      <p className="score-note">(includes {game.komi} komi for white)</p>
                    </div>
                  )}
                  {scoreResult.captures && (
                    <div className="captures-info">
                      <h4>Captures</h4>
                      <div>Black captured: {scoreResult.captures.black}</div>
                      <div>White captured: {scoreResult.captures.white}</div>
                    </div>
                  )}
                  {scoreResult.deadStones && (
                    <div className="dead-stones-info">
                      <h4>Dead Stones</h4>
                      <div>Black dead: {scoreResult.deadStones.blackDeadStones.length}</div>
                      <div>White dead: {scoreResult.deadStones.whiteDeadStones.length}</div>
                    </div>
                  )}
                  <button
                    onClick={resetScore}
                    className="button button-secondary reset-score-button"
                  >
                    Reset Score
                  </button>
                </div>
              </div>
            )}
          </div>
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
            disabled={fixMode || calculatingScore}
            showMoveNumbers={showMoveNumbers}
            onShowMoveNumbersChange={setShowMoveNumbers}
            aiLevel={aiLevel}
            onAiLevelChange={setAiLevel}
            onGetAiMove={handleGetAiMove}
            aiMoveLoading={aiMoveLoading}
            consecutivePasses={consecutivePasses}
          />

          {/* Fix Territory Mode */}
          {fixMode && (
            <div className="fix-territory-panel">
              <div className="fix-instructions">
                Click on stones you believe are dead (prisoners). Dead stones count toward
                the opponent's score. Click again to unmark.
              </div>

              <div className="marked-count">
                {markedDeadStones.length} stone{markedDeadStones.length !== 1 ? 's' : ''} marked as dead
              </div>

              {scoreError && <div className="error score-error">{scoreError}</div>}

              <div className="fix-buttons">
                <button
                  onClick={handleCalculateScore}
                  className="button button-primary"
                  disabled={calculatingScore}
                >
                  {calculatingScore ? 'Calculating...' : 'Calculate Score'}
                </button>
                <button
                  onClick={clearMarkedDeadStones}
                  className="button button-secondary"
                  disabled={calculatingScore || markedDeadStones.length === 0}
                >
                  Clear Marks
                </button>
                <button
                  onClick={toggleFixMode}
                  className="button button-secondary"
                  disabled={calculatingScore}
                >
                  Cancel
                </button>
              </div>
            </div>
          )}

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
            <div className="info-row">
              <span className="label">Komi:</span>
              <span className="value">{game.komi}</span>
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
