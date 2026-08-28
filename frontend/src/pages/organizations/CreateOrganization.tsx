import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../api/client';

export default function CreateOrganization() {
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    
    try {
      await apiClient.post('/api/v1/organizations', { name });
      navigate('/organizations');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create organization');
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <div className="page-header">
        <h1 className="page-title">Create Organization</h1>
      </div>

      <div className="card" style={{ padding: '32px' }}>
        {error && (
          <div style={{ padding: '16px', backgroundColor: 'var(--danger-color)', color: 'white', borderRadius: 'var(--border-radius)', marginBottom: '24px' }}>
            {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="name">Organization Name</label>
            <input
              id="name"
              type="text"
              className="form-control"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              placeholder="e.g. Acme Corp"
            />
          </div>
          
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '16px' }}>
            <button type="button" className="btn" onClick={() => navigate('/organizations')}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Creating...' : 'Create Organization'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
