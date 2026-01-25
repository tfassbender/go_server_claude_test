import './GameControls.css';

interface GameControlsProps {
  onPass: () => void;
  onResign: () => void;
  disabled?: boolean;
}

const GameControls = ({ onPass, onResign, disabled = false }: GameControlsProps) => {
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
      </div>
      <div className="control-help">
        <p><strong>Pass:</strong> Skip your turn. Game ends after 2 consecutive passes.</p>
        <p><strong>Resign:</strong> Forfeit the game.</p>
      </div>
    </div>
  );
};

export default GameControls;
