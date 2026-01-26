import './FixTerritoryControls.css';

interface FixTerritoryControlsProps {
  fixMode: boolean;
  markedCount: number;
  onToggleFixMode: () => void;
  onRecalculate: () => void;
  onClear: () => void;
  loading: boolean;
  error: string;
}

const FixTerritoryControls = ({
  fixMode,
  markedCount,
  onToggleFixMode,
  onRecalculate,
  onClear,
  loading,
  error
}: FixTerritoryControlsProps) => {
  if (!fixMode) {
    return (
      <div className="fix-territory-controls">
        <button
          onClick={onToggleFixMode}
          className="button button-secondary fix-territory-button"
          title="Manually mark dead stones to recalculate the score"
        >
          Fix Territory
        </button>
      </div>
    );
  }

  return (
    <div className="fix-territory-controls fix-mode-active">
      <div className="fix-instructions">
        Click on stones you believe are dead (prisoners). Dead stones count toward
        the opponent's score. Click again to unmark.
      </div>

      <div className="marked-count">
        {markedCount} stone{markedCount !== 1 ? 's' : ''} marked as dead
      </div>

      {error && <div className="error fix-error">{error}</div>}

      <div className="fix-buttons">
        <button
          onClick={onRecalculate}
          className="button button-primary"
          disabled={loading}
        >
          {loading ? 'Calculating...' : 'Recalculate Score'}
        </button>
        <button
          onClick={onClear}
          className="button button-secondary"
          disabled={loading || markedCount === 0}
        >
          Clear Marks
        </button>
        <button
          onClick={onToggleFixMode}
          className="button button-secondary"
          disabled={loading}
        >
          Cancel
        </button>
      </div>
    </div>
  );
};

export default FixTerritoryControls;
