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
            <p><strong>Winner:</strong> {game.result.winner === 'black' ? game.blackPlayer : game.whitePlayer}</p>
            <p><strong>Method:</strong> {game.result.method}</p>
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
