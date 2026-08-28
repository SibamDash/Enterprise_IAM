import { useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient, setTenantId } from '../../api/client';

export default function ForgotPassword() {
  const [tenantId, setTenant] = useState(localStorage.getItem('tenantId') || '');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      setTenantId(tenantId);
      localStorage.setItem('tenantId', tenantId);
      
      await apiClient.post('/api/v1/auth/forgot-password', { email });
      setSuccess(true);
    } catch (err) {
      // Intentionally show success to prevent email enumeration
      setSuccess(true);
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div style={{ textAlign: 'center' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '16px' }}>Check your email</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
          If an account exists for {email}, we have sent password reset instructions.
        </p>
        <Link to="/login" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
          Return to login
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', marginBottom: '8px', textAlign: 'center' }}>Forgot password?</h2>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '24px', textAlign: 'center', fontSize: '0.875rem' }}>
        Enter your email and we'll send you a link to reset your password.
      </p>

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label" htmlFor="tenantId">Organization ID (Tenant)</label>
          <input
            id="tenantId"
            type="text"
            className="form-control"
            value={tenantId}
            onChange={(e) => setTenant(e.target.value)}
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
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center', marginBottom: '16px' }} disabled={loading}>
          {loading ? 'Sending...' : 'Send reset link'}
        </button>
        
        <div style={{ textAlign: 'center' }}>
          <Link to="/login" style={{ fontSize: '0.875rem', color: 'var(--primary-color)' }}>
            Back to login
          </Link>
        </div>
      </form>
    </div>
  );
}
