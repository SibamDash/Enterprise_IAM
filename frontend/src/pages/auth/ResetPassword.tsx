import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { apiClient } from '../../api/client';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match');
      setLoading(false);
      return;
    }

    try {
      await apiClient.post('/api/v1/auth/reset-password', {
        token,
        newPassword
      });
      navigate('/login');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reset password. The link may be expired.');
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div style={{ textAlign: 'center' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '16px', color: 'var(--danger-color)' }}>Invalid Link</h2>
        <p style={{ color: 'var(--text-secondary)' }}>
          This password reset link is invalid or missing a token.
        </p>
      </div>
    );
  }

  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', marginBottom: '24px', textAlign: 'center' }}>Create new password</h2>
      
      {error && (
        <div style={{ padding: '12px', backgroundColor: 'var(--danger-color)', color: 'white', borderRadius: 'var(--border-radius)', marginBottom: '24px', fontSize: '0.875rem' }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label" htmlFor="newPassword">New Password</label>
          <input
            id="newPassword"
            type="password"
            className="form-control"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            minLength={8}
          />
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="confirmPassword">Confirm Password</label>
          <input
            id="confirmPassword"
            type="password"
            className="form-control"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            minLength={8}
          />
        </div>

        <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={loading}>
          {loading ? 'Resetting...' : 'Reset password'}
        </button>
      </form>
    </div>
  );
}
