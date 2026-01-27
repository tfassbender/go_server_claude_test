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
}: ForkControlsProps) => {
  const displayMoveNumber = currentForkMoveIndex + 1;

  return (
    <div className="fork-controls">
      <div className="turn-indicator">
        <span className="turn-label">Next:</span>
        <span className={`turn-stone ${nextTurn}`} />
        <span className="turn-color">{nextTurn === 'black' ? 'Black' : 'White'}</span>
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
            disabled={disabled}
            title="Pass turn without placing a stone"
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
