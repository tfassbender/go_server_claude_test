import { Game } from '../../types/Game';
import './GameInfo.css';

interface GameInfoProps {
  game: Game;
  currentUsername: string;
}

const GameInfo = ({ game, currentUsername }: GameInfoProps) => {
  const yourColor = currentUsername === game.blackPlayer ? 'black' : 'white';
  const isYourTurn = game.currentTurn === yourColor;

  return (
    <div className="game-info-panel card">
      <h2>Game Information</h2>

      <div className="info-section">
        <div className="info-row">
          <span className="label">Board Size:</span>
          <span className="value">{game.boardSize}x{game.boardSize}</span>
        </div>

        <div className="info-row">
          <span className="label">Status:</span>
          <span className={`value status-${game.status}`}>
            {game.status.charAt(0).toUpperCase() + game.status.slice(1)}
          </span>
        </div>
      </div>

      <div className="info-section">
        <h3>Players</h3>
        <div className="player-info">
          <div className={`player ${yourColor === 'black' ? 'you' : ''}`}>
            <span className="color-dot black"></span>
            <span>{game.blackPlayer} {yourColor === 'black' && '(You)'}</span>
          </div>
          <div className={`player ${yourColor === 'white' ? 'you' : ''}`}>
            <span className="color-dot white"></span>
            <span>{game.whitePlayer} {yourColor === 'white' && '(You)'}</span>
          </div>
        </div>
      </div>

      {game.status === 'active' && (
        <div className="info-section">
          <div className={`turn-indicator ${isYourTurn ? 'your-turn' : 'opponent-turn'}`}>
            {isYourTurn ? '🟢 Your Turn' : `⚪ ${game.currentTurn === 'black' ? game.blackPlayer : game.whitePlayer}'s Turn`}
          </div>
        </div>
      )}

      {game.status === 'completed' && game.result && (
        <div className="info-section">
          <h3>Result</h3>
          <div className="result-info">
            <p className="winner-summary">
              {(() => {
                const winnerName = game.result.winner === 'black' ? game.blackPlayer : game.whitePlayer;
                const winnerColor = game.result.winner.charAt(0).toUpperCase() + game.result.winner.slice(1);

                if (game.result.method === 'resignation') {
                  return `${winnerColor} (${winnerName}) wins by resignation`;
                } else if (game.result.method === 'score' && game.result.score) {
                  const margin = Math.abs(game.result.score.black - game.result.score.white);
                  return `${winnerColor} (${winnerName}) wins by ${margin} points`;
                } else {
                  return `${winnerColor} (${winnerName}) wins`;
                }
              })()}
            </p>
            {game.result.score && (
              <div className="score-breakdown">
                <h4>Final Score</h4>
                <div className="score-row">
                  <span className="score-label">Black:</span>
                  <span className="score-value">{game.result.score.black}</span>
                </div>
                <div className="score-row">
                  <span className="score-label">White:</span>
                  <span className="score-value">{game.result.score.white}</span>
                </div>
                <p className="score-note">(includes 5.5 komi for white)</p>
              </div>
            )}
            {game.result.captures && (
              <div className="captures-breakdown">
                <h4>Captured Stones</h4>
                <div className="score-row">
                  <span className="score-label">Black:</span>
                  <span className="score-value">{game.result.captures.black}</span>
                </div>
                <div className="score-row">
                  <span className="score-label">White:</span>
                  <span className="score-value">{game.result.captures.white}</span>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="info-section">
        <div className="info-row">
          <span className="label">Consecutive Passes:</span>
          <span className="value">{game.passes}/2</span>
        </div>
        <div className="info-row">
          <span className="label">Moves:</span>
          <span className="value">{game.moves.length}</span>
        </div>
      </div>
    </div>
  );
};

export default GameInfo;
