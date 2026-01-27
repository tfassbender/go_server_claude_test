import { Move } from '../../types/Move';
import './AnalysisControls.css';

interface AnalysisControlsProps {
  currentMoveIndex: number;
  totalMoves: number;
  moves: Move[];
  showMoveNumbers: boolean;
  showTerritory: boolean;
  onShowMoveNumbersChange: (show: boolean) => void;
  onShowTerritoryChange: (show: boolean) => void;
  onGoToMove: (index: number) => void;
}

const AnalysisControls = ({
  currentMoveIndex,
  totalMoves,
  moves,
  showMoveNumbers,
  showTerritory,
  onShowMoveNumbersChange,
  onShowTerritoryChange,
  onGoToMove
}: AnalysisControlsProps) => {
  const isAtStart = currentMoveIndex < 0;
  const isAtEnd = currentMoveIndex >= totalMoves - 1;
  const atFinalPosition = currentMoveIndex === totalMoves - 1;

  const goToStart = () => onGoToMove(-1);
  const goToPrevious = () => onGoToMove(Math.max(-1, currentMoveIndex - 1));
  const goToNext = () => onGoToMove(Math.min(totalMoves - 1, currentMoveIndex + 1));
  const goToEnd = () => onGoToMove(totalMoves - 1);

  const handleSliderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onGoToMove(parseInt(e.target.value, 10));
  };

  // Get current move for pass indicator
  const currentMove = currentMoveIndex >= 0 && currentMoveIndex < moves.length
    ? moves[currentMoveIndex]
    : null;
  const isPassMove = currentMove?.action === 'pass';
  const passPlayer = isPassMove ? currentMove.player : null;

  // Display move number (1-based for user display)
  const displayMoveNumber = currentMoveIndex + 1;

  return (
    <div className="analysis-controls">
      <div className="navigation-row">
        <button
          className="nav-button"
          onClick={goToStart}
          disabled={isAtStart}
          title="Go to start (Home)"
        >
          ⏮
        </button>
        <button
          className="nav-button"
          onClick={goToPrevious}
          disabled={isAtStart}
          title="Previous move (←)"
        >
          ◀
        </button>

        <span className="move-indicator">
          {currentMoveIndex < 0 ? 'Start' : `Move ${displayMoveNumber}`} of {totalMoves}
        </span>

        <button
          className="nav-button"
          onClick={goToNext}
          disabled={isAtEnd}
          title="Next move (→)"
        >
          ▶
        </button>
        <button
          className="nav-button"
          onClick={goToEnd}
          disabled={isAtEnd}
          title="Go to end (End)"
        >
          ⏭
        </button>
      </div>

      <div className="slider-row">
        <input
          type="range"
          className="move-slider"
          min={-1}
          max={totalMoves - 1}
          value={currentMoveIndex}
          onChange={handleSliderChange}
        />
      </div>

      {isPassMove && (
        <div className="pass-indicator">
          Move {displayMoveNumber}: {passPlayer === 'black' ? 'Black' : 'White'} passed
        </div>
      )}

      <div className="options-row">
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={showMoveNumbers}
            onChange={(e) => onShowMoveNumbersChange(e.target.checked)}
          />
          Show move numbers
        </label>

        <label className={`checkbox-label ${!atFinalPosition ? 'disabled' : ''}`}>
          <input
            type="checkbox"
            checked={showTerritory}
            onChange={(e) => onShowTerritoryChange(e.target.checked)}
            disabled={!atFinalPosition}
          />
          Show territory
          {!atFinalPosition && <span className="hint">(available at end)</span>}
        </label>
      </div>
    </div>
  );
};

export default AnalysisControls;
