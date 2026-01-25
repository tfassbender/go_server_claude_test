import { Link } from 'react-router-dom';
import './Header.css';

interface HeaderProps {
  username: string;
  onLogout: () => void;
}

const Header = ({ username, onLogout }: HeaderProps) => {
  return (
    <header className="header">
      <div className="header-content">
        <Link to="/lobby" className="logo">Go Game</Link>

        <nav className="nav">
          <Link to="/lobby" className="nav-link">Games</Link>
          <Link to="/create-game" className="nav-link">New Game</Link>
        </nav>

        <div className="user-section">
          <span className="username">{username}</span>
          <button onClick={onLogout} className="button button-secondary">
            Logout
          </button>
        </div>
      </div>
    </header>
  );
};

export default Header;
