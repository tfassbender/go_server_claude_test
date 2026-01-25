import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import RegisterForm from '../components/Auth/RegisterForm';
import './Auth.css';

const Register = () => {
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleRegister = async (username: string, password: string) => {
    try {
      setError('');
      setSuccess('');
      await authService.register({ username, password });
      setSuccess('Account created successfully! Redirecting to login...');

      setTimeout(() => {
        navigate('/login');
      }, 2000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-container">
        <h1>Go Game</h1>
        <h2>Register</h2>

        <RegisterForm onRegister={handleRegister} error={error} />

        {success && <div className="success">{success}</div>}

        <div className="auth-footer">
          <p>
            Already have an account? <Link to="/login">Login here</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;
