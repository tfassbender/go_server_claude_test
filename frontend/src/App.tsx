import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import GameLobby from './pages/GameLobby';
import CreateGame from './pages/CreateGame';
import GamePlay from './pages/GamePlay';
import GameAnalysis from './pages/GameAnalysis';
import Header from './components/Layout/Header';
import './App.css';

function AppRoutes() {
  const { isAuthenticated, isLoading, username, logout } = useAuth();

  if (isLoading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <>
      <div className="app">
        {isAuthenticated && <Header username={username || ''} onLogout={logout} />}

        <Routes>
          <Route path="/login" element={
            isAuthenticated ? <Navigate to="/lobby" /> : <Login />
          } />

          <Route path="/register" element={
            isAuthenticated ? <Navigate to="/lobby" /> : <Register />
          } />

          <Route path="/lobby" element={
            isAuthenticated ? <GameLobby /> : <Navigate to="/login" />
          } />

          <Route path="/create-game" element={
            isAuthenticated ? <CreateGame /> : <Navigate to="/login" />
          } />

          <Route path="/game/:gameId" element={
            isAuthenticated ? <GamePlay /> : <Navigate to="/login" />
          } />

          <Route path="/analyze/:gameId" element={
            isAuthenticated ? <GameAnalysis /> : <Navigate to="/login" />
          } />

          <Route path="/" element={
            <Navigate to={isAuthenticated ? "/lobby" : "/login"} />
          } />
        </Routes>
      </div>
    </>
  );
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </Router>
  );
}

export default App;
