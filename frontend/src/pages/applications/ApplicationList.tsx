import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { LayoutDashboard, Plus, Trash2, Key, Edit } from 'lucide-react';
import { apiClient } from '../../api/client';
import SecretDisplayModal from './SecretDisplayModal';

interface ClientDto {
  id: string;
  clientId: string;
  clientName: string;
  clientIdIssuedAt: string;
  clientAuthenticationMethods: string[];
  authorizationGrantTypes: string[];
  redirectUris: string[];
  postLogoutRedirectUris: string[];
  scopes: string[];
}

export default function ApplicationList() {
  const [clients, setClients] = useState<ClientDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [secretModal, setSecretModal] = useState<{clientId: string, clientSecret: string} | null>(null);

  const fetchClients = () => {
    setLoading(true);
    apiClient.get('/api/v1/clients')
      .then(res => {
        setClients(res.data);
        setError('');
      })
      .catch(err => {
        console.error(err);
        setError('Failed to fetch applications.');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchClients();
  }, []);

  const handleDelete = (id: string) => {
    if (!window.confirm('Are you sure you want to delete this application? This action cannot be undone and will break any integrations using this client ID.')) return;
    
    apiClient.delete(`/api/v1/clients/${id}`)
      .then(() => fetchClients())
      .catch(err => {
        console.error(err);
        alert('Failed to delete application.');
      });
  };

  const handleRotateSecret = (id: string) => {
    if (!window.confirm('Are you sure you want to rotate the client secret? The old secret will immediately stop working.')) return;
    
    apiClient.post(`/api/v1/clients/${id}/rotate-secret`)
      .then(res => {
        setSecretModal({
          clientId: res.data.clientId,
          clientSecret: res.data.clientSecret
        });
      })
      .catch(err => {
        console.error(err);
        alert('Failed to rotate secret.');
      });
  };

  if (loading) return <div>Loading applications...</div>;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Applications</h1>
          <p className="subtitle">Manage OAuth 2.0 and OpenID Connect applications</p>
        </div>
        <div className="actions">
          <Link to="/applications/new" className="btn btn-primary">
            <Plus size={20} />
            Register Application
          </Link>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Client ID</th>
              <th>Auth Methods</th>
              <th>Grant Types</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {clients.map(client => (
              <tr key={client.id}>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: '500' }}>
                    <LayoutDashboard size={18} className="text-secondary" />
                    {client.clientName}
                  </div>
                </td>
                <td><code style={{ fontSize: '12px', background: '#f1f5f9', padding: '2px 6px', borderRadius: '4px' }}>{client.clientId}</code></td>
                <td>
                  <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                    {client.clientAuthenticationMethods.map(m => (
                      <span key={m} className="badge badge-secondary">{m}</span>
                    ))}
                  </div>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                    {client.authorizationGrantTypes.map(g => (
                      <span key={g} className="badge badge-info">{g}</span>
                    ))}
                  </div>
                </td>
                <td>{new Date(client.clientIdIssuedAt).toLocaleDateString()}</td>
                <td>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <Link to={`/applications/${client.id}/edit`} className="btn btn-secondary btn-sm" title="Edit">
                      <Edit size={16} />
                    </Link>
                    <button 
                      className="btn btn-secondary btn-sm" 
                      onClick={() => handleRotateSecret(client.id)}
                      title="Rotate Secret"
                    >
                      <Key size={16} />
                    </button>
                    <button 
                      className="btn btn-danger btn-sm" 
                      onClick={() => handleDelete(client.id)}
                      title="Delete"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {clients.length === 0 && (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '32px', color: 'var(--text-secondary)' }}>
                  No applications found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {secretModal && (
        <SecretDisplayModal 
          clientId={secretModal.clientId}
          clientSecret={secretModal.clientSecret}
          onClose={() => setSecretModal(null)}
        />
      )}
    </div>
  );
}
