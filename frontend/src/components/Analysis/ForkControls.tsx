import { StoneColor } from '../../utils/goRulesEngine';
import './ForkControls.css';

interface ForkControlsProps {
  nextTurn: StoneColor;
  forkMoveCount: number;
  currentForkMoveIndex: number;
  canUndo: boolean;
  canRedo: boolean;
  onUndo: () => void;
  onRedo: () => void;
  onClearAll: () => void;
  onPass: () => void;
  disabled?: boolean;
  showMoveNumbers: boolean;
  onShowMoveNumbersChange: (show: boolean) => void;
  aiLevel: number;
  onAiLevelChange: (level: number) => void;
  onGetAiMove: () => void;
  aiMoveLoading?: boolean;
  consecutivePasses: number;
}

const ForkControls = ({
  nextTurn,
  forkMoveCount,
  currentForkMoveIndex,
  canUndo,
  canRedo,
  onUndo,
  onRedo,
  onClearAll,
  onPass,
  disabled = false,
  showMoveNumbers,
  onShowMoveNumbersChange,
  aiLevel,
  onAiLevelChange,
  onGetAiMove,
  aiMoveLoading = false,
  consecutivePasses,
}: ForkControlsProps) => {
  const displayMoveNumber = currentForkMoveIndex + 1;
  const gameEnded = consecutivePasses >= 2;

  return (
    <div className="fork-controls">
      <div className="turn-indicator">
        <span className="turn-label">Next:</span>
        <span className={`turn-stone ${nextTurn}`} />
        <span className="turn-color">{nextTurn === 'black' ? 'Black' : 'White'}</span>
      </div>

      <div className="consecutive-passes-indicator">
        <span className="passes-label">Consecutive Passes:</span>
        <span className={`passes-count ${gameEnded ? 'ended' : ''}`}>
          {consecutivePasses}/2
        </span>
        {gameEnded && <span className="game-ended-text">Game ended</span>}
      </div>

      <div className="fork-move-counter">
        {forkMoveCount === 0 ? (
          'No fork moves yet'
        ) : (
          <>Fork move {displayMoveNumber} of {forkMoveCount}</>
        )}
      </div>

      <div className="fork-control-buttons">
        <div className="button-row">
          <button
            className="fork-button"
            onClick={onUndo}
            disabled={disabled || !canUndo}
            title="Undo (Ctrl+Z)"
          >
            Undo
          </button>
          <button
            className="fork-button"
            onClick={onRedo}
            disabled={disabled || !canRedo}
            title="Redo (Ctrl+Y)"
          >
            Redo
          </button>
        </div>
        <div className="button-row">
          <button
            className="fork-button secondary"
            onClick={onPass}
            disabled={disabled || gameEnded || aiMoveLoading}
            title={gameEnded ? "Game has ended (2 consecutive passes)" : aiMoveLoading ? "Waiting for AI move..." : "Pass turn without placing a stone"}
          >
            Pass
          </button>
          <button
            className="fork-button danger"
            onClick={onClearAll}
            disabled={disabled || forkMoveCount === 0}
            title="Clear all fork moves"
          >
            Clear All
          </button>
        </div>
      </div>

      <div className="ai-section">
        <div className="ai-section-title">AI Move Suggestion</div>
        <div className="ai-controls">
          <label htmlFor="ai-level" className="ai-level-label">
            AI Strength:
          </label>
          <select
            id="ai-level"
            className="ai-level-select"
            value={aiLevel}
            onChange={(e) => onAiLevelChange(Number(e.target.value))}
            disabled={disabled}
          >
            <option value={1}>Easy (15-20 kyu)</option>
            <option value={3}>Casual (12-15 kyu)</option>
            <option value={5}>Medium (10-12 kyu)</option>
            <option value={7}>Hard (8-10 kyu)</option>
            <option value={10}>Maximum (5-8 kyu)</option>
          </select>
          <button
            className="fork-button ai-move-button"
            onClick={onGetAiMove}
            disabled={disabled || aiMoveLoading || gameEnded}
            title={gameEnded ? "Game has ended (2 consecutive passes)" : "Get AI move suggestion for the current position"}
          >
            {aiMoveLoading ? 'Thinking...' : 'Get AI Move'}
          </button>
        </div>
      </div>

      <div className="options-row">
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={showMoveNumbers}
            onChange={(e) => onShowMoveNumbersChange(e.target.checked)}
          />
          Show move numbers
        </label>
      </div>

      <div className="fork-hints">
        <div className="hint-row">
          <span className="hint-key">Click</span>
          <span className="hint-desc">Place stone</span>
        </div>
        <div className="hint-row">
          <span className="hint-key">Ctrl+Z</span>
          <span className="hint-desc">Undo</span>
        </div>
        <div className="hint-row">
          <span className="hint-key">Ctrl+Y</span>
          <span className="hint-desc">Redo</span>
        </div>
      </div>
    </div>
  );
};

export default ForkControls;
