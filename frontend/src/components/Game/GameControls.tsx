import './GameControls.css';

interface GameControlsProps {
  onPass: () => void;
  onResign: () => void;
  onUndo?: () => void;
  allowUndo?: boolean;
  disabled?: boolean;
}

const GameControls = ({ onPass, onResign, onUndo, allowUndo = false, disabled = false }: GameControlsProps) => {
  const handleResign = () => {
    if (window.confirm('Are you sure you want to resign? This cannot be undone.')) {
      onResign();
    }
  };

  return (
    <div className="game-controls card">
      <h3>Actions</h3>
      <div className="control-buttons">
        <button
          onClick={onPass}
          className="button button-secondary"
          disabled={disabled}
        >
          Pass
        </button>
        <button
          onClick={handleResign}
          className="button button-danger"
          disabled={disabled}
        >
          Resign
        </button>
        {allowUndo && onUndo && (
          <button
            onClick={onUndo}
            className="button button-primary"
            disabled={disabled}
          >
            Undo
          </button>
        )}
      </div>
      <div className="control-help">
        <p><strong>Pass:</strong> Skip your turn. Game ends after 2 consecutive passes.</p>
        <p><strong>Resign:</strong> Forfeit the game.</p>
        {allowUndo && <p><strong>Undo:</strong> Take back your last move and the AI's response.</p>}
      </div>
    </div>
  );
};

export default GameControls;
