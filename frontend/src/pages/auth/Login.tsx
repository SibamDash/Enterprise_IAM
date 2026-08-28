import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { apiClient, setTenantId, setTokens } from '../../api/client';

export default function Login() {
  const [formData, setFormData] = useState({
    tenantId: localStorage.getItem('tenantId') || '',
    email: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      setTenantId(formData.tenantId);
      localStorage.setItem('tenantId', formData.tenantId);
      
      const response = await apiClient.post('/api/v1/auth/login', {
        email: formData.email,
        password: formData.password
      });

      const { accessToken, refreshToken } = response.data;
      if (accessToken && refreshToken) {
        setTokens(accessToken, refreshToken);
      }

      navigate('/');
    } catch (err: any) {
      if (err.response?.status === 401) {
        setError('Invalid credentials or account locked.');
      } else {
        setError('An error occurred during login. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.id]: e.target.value
    }));
  };

  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', marginBottom: '24px', textAlign: 'center' }}>Sign In</h2>
      
      {error && (
        <div style={{ padding: '12px', backgroundColor: 'var(--danger-color)', color: 'white', borderRadius: 'var(--border-radius)', marginBottom: '24px', fontSize: '0.875rem' }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label" htmlFor="tenantId">Organization ID (Tenant)</label>
          <input
            id="tenantId"
            type="text"
            className="form-control"
            value={formData.tenantId}
            onChange={handleChange}
            required
            placeholder="UUID of your organization"
          />
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="email">Email address</label>
          <input
            id="email"
            type="email"
            className="form-control"
            value={formData.email}
            onChange={handleChange}
            required
          />
        </div>

        <div className="form-group">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <label className="form-label" htmlFor="password" style={{ marginBottom: 0 }}>Password</label>
            <Link to="/forgot-password" style={{ fontSize: '0.875rem', color: 'var(--primary-color)' }}>
              Forgot password?
            </Link>
          </div>
          <input
            id="password"
            type="password"
            className="form-control"
            value={formData.password}
            onChange={handleChange}
            required
          />
        </div>

        <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={loading}>
          {loading ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </div>
  );
}
