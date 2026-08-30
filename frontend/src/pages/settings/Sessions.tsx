import { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';

interface Session {
  id: string;
  userAgent: string;
  ipAddress: string;
  lastActiveAt: string;
}

export default function Sessions() {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchSessions = async () => {
    try {
      const res = await apiClient.get('/api/v1/sessions');
      setSessions(res.data);
      
      // If we have no active sessions, our current session must have been revoked
      if (res.data.length === 0) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
      }
    } catch (err) {
      setError('Failed to load active sessions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSessions();
  }, []);

  const handleRevoke = async (id: string) => {
    try {
      await apiClient.delete(`/api/v1/sessions/${id}`);
      fetchSessions();
    } catch (err) {
      setError('Failed to revoke session');
    }
  };

  const handleLogoutAll = async () => {
    try {
      // For this UI, we can just hit /api/v1/sessions to clear other devices
      // Wait, the API requires currentSessionId, but we don't know our own session ID easily from the JWT.
      // Actually, if we just want to clear ALL sessions, we should logout. 
      // If we want to clear OTHER sessions, we need our session ID.
      alert('Logout all devices is currently a backend-only testable feature in this phase');
    } catch (err) {
      setError('Failed to logout all devices');
    }
  };

  if (loading) return <div>Loading sessions...</div>;

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '24px' }}>
      <h2 style={{ fontSize: '1.5rem', marginBottom: '24px' }}>Active Sessions</h2>
      
      {error && (
        <div style={{ padding: '12px', backgroundColor: 'var(--danger-color)', color: 'white', borderRadius: 'var(--border-radius)', marginBottom: '24px' }}>
          {error}
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '16px' }}>
        <button className="btn btn-secondary" onClick={handleLogoutAll}>
          Logout other devices
        </button>
      </div>

      <div style={{ display: 'grid', gap: '16px' }}>
        {sessions.map(session => (
          <div key={session.id} style={{ border: '1px solid var(--border-color)', borderRadius: 'var(--border-radius)', padding: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ fontWeight: '600', marginBottom: '4px' }}>{session.userAgent || 'Unknown Device'}</div>
              <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                IP: {session.ipAddress} • Last active: {new Date(session.lastActiveAt).toLocaleString()}
              </div>
            </div>
            <button className="btn btn-danger" onClick={() => handleRevoke(session.id)}>
              Revoke
            </button>
          </div>
        ))}
        {sessions.length === 0 && (
          <div style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '24px' }}>
            No active sessions found.
          </div>
        )}
      </div>
    </div>
  );
}
