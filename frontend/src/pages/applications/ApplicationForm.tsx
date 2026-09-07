import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Save } from 'lucide-react';
import { apiClient } from '../../api/client';
import SecretDisplayModal from './SecretDisplayModal';

export default function ApplicationForm() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEditMode = !!id;

  const [loading, setLoading] = useState(isEditMode);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  
  const [secretModal, setSecretModal] = useState<{clientId: string, clientSecret: string} | null>(null);

  const [formData, setFormData] = useState({
    clientId: '',
    clientName: '',
    clientAuthenticationMethods: ['client_secret_basic'],
    authorizationGrantTypes: ['authorization_code'],
    redirectUris: '',
    postLogoutRedirectUris: '',
    scopes: 'openid,profile'
  });

  useEffect(() => {
    if (isEditMode) {
      apiClient.get(`/api/v1/clients/${id}`)
        .then(res => {
          const client = res.data;
          setFormData({
            clientId: client.clientId,
            clientName: client.clientName,
            clientAuthenticationMethods: client.clientAuthenticationMethods,
            authorizationGrantTypes: client.authorizationGrantTypes,
            redirectUris: client.redirectUris.join('\n'),
            postLogoutRedirectUris: client.postLogoutRedirectUris.join('\n'),
            scopes: client.scopes.join(',')
          });
        })
        .catch(err => {
          console.error(err);
          setError('Failed to fetch application details.');
        })
        .finally(() => setLoading(false));
    }
  }, [id, isEditMode]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');

    const payload = {
      clientId: formData.clientId,
      clientName: formData.clientName,
      clientAuthenticationMethods: formData.clientAuthenticationMethods,
      authorizationGrantTypes: formData.authorizationGrantTypes,
      redirectUris: formData.redirectUris.split('\n').map(u => u.trim()).filter(Boolean),
      postLogoutRedirectUris: formData.postLogoutRedirectUris.split('\n').map(u => u.trim()).filter(Boolean),
      scopes: formData.scopes.split(',').map(s => s.trim()).filter(Boolean)
    };

    const request = isEditMode
      ? apiClient.put(`/api/v1/clients/${id}`, payload)
      : apiClient.post('/api/v1/clients', payload);

    request
      .then(res => {
        if (!isEditMode && res.data.clientSecret) {
          setSecretModal({
            clientId: res.data.clientId,
            clientSecret: res.data.clientSecret
          });
        } else {
          navigate('/applications');
        }
      })
      .catch(err => {
        console.error(err);
        setError(err.response?.data?.message || 'Failed to save application. Ensure client ID is unique.');
      })
      .finally(() => setSaving(false));
  };

  const handleCheckboxChange = (field: 'clientAuthenticationMethods' | 'authorizationGrantTypes', value: string, checked: boolean) => {
    setFormData(prev => {
      const current = prev[field];
      if (checked && !current.includes(value)) {
        return { ...prev, [field]: [...current, value] };
      } else if (!checked && current.includes(value)) {
        return { ...prev, [field]: current.filter(v => v !== value) };
      }
      return prev;
    });
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="page" style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <button className="btn btn-secondary" onClick={() => navigate('/applications')} style={{ padding: '8px' }}>
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1>{isEditMode ? 'Edit Application' : 'Register Application'}</h1>
            <p className="subtitle">Configure OAuth 2.0 settings for this client</p>
          </div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <form onSubmit={handleSubmit} className="form-layout">
          <div className="form-group">
            <label>Application Name <span className="required">*</span></label>
            <input 
              type="text" 
              className="form-control" 
              value={formData.clientName}
              onChange={e => setFormData({...formData, clientName: e.target.value})}
              required
              placeholder="e.g. Acme CRM"
            />
          </div>

          <div className="form-group">
            <label>Client ID <span className="required">*</span></label>
            <input 
              type="text" 
              className="form-control" 
              value={formData.clientId}
              onChange={e => setFormData({...formData, clientId: e.target.value})}
              required
              disabled={isEditMode}
              placeholder="e.g. acme-crm-web"
            />
            {isEditMode && <small className="help-text">Client ID cannot be changed after creation.</small>}
          </div>

          <div className="form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginTop: '16px' }}>
            <div className="form-group">
              <label>Authentication Methods</label>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '8px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 'normal' }}>
                  <input 
                    type="checkbox" 
                    checked={formData.clientAuthenticationMethods.includes('client_secret_basic')}
                    onChange={e => handleCheckboxChange('clientAuthenticationMethods', 'client_secret_basic', e.target.checked)}
                  />
                  Client Secret Basic
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 'normal' }}>
                  <input 
                    type="checkbox" 
                    checked={formData.clientAuthenticationMethods.includes('client_secret_post')}
                    onChange={e => handleCheckboxChange('clientAuthenticationMethods', 'client_secret_post', e.target.checked)}
                  />
                  Client Secret Post
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 'normal' }}>
                  <input 
                    type="checkbox" 
                    checked={formData.clientAuthenticationMethods.includes('none')}
                    onChange={e => handleCheckboxChange('clientAuthenticationMethods', 'none', e.target.checked)}
                  />
                  None (Public Client)
                </label>
              </div>
            </div>

            <div className="form-group">
              <label>Allowed Grant Types</label>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '8px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 'normal' }}>
                  <input 
                    type="checkbox" 
                    checked={formData.authorizationGrantTypes.includes('authorization_code')}
                    onChange={e => handleCheckboxChange('authorizationGrantTypes', 'authorization_code', e.target.checked)}
                  />
                  Authorization Code
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 'normal' }}>
                  <input 
                    type="checkbox" 
                    checked={formData.authorizationGrantTypes.includes('client_credentials')}
                    onChange={e => handleCheckboxChange('authorizationGrantTypes', 'client_credentials', e.target.checked)}
                  />
                  Client Credentials
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 'normal' }}>
                  <input 
                    type="checkbox" 
                    checked={formData.authorizationGrantTypes.includes('refresh_token')}
                    onChange={e => handleCheckboxChange('authorizationGrantTypes', 'refresh_token', e.target.checked)}
                  />
                  Refresh Token
                </label>
              </div>
            </div>
          </div>

          <div className="form-group" style={{ marginTop: '16px' }}>
            <label>Allowed Redirect URIs</label>
            <textarea 
              className="form-control" 
              value={formData.redirectUris}
              onChange={e => setFormData({...formData, redirectUris: e.target.value})}
              rows={3}
              placeholder="http://localhost:3000/callback&#10;https://app.example.com/oauth2/code"
            />
            <small className="help-text">Enter one URI per line.</small>
          </div>

          <div className="form-group">
            <label>Post Logout Redirect URIs</label>
            <textarea 
              className="form-control" 
              value={formData.postLogoutRedirectUris}
              onChange={e => setFormData({...formData, postLogoutRedirectUris: e.target.value})}
              rows={2}
              placeholder="http://localhost:3000/"
            />
            <small className="help-text">Enter one URI per line.</small>
          </div>

          <div className="form-group">
            <label>Scopes</label>
            <input 
              type="text" 
              className="form-control" 
              value={formData.scopes}
              onChange={e => setFormData({...formData, scopes: e.target.value})}
              placeholder="openid, profile, email"
            />
            <small className="help-text">Comma-separated list of scopes.</small>
          </div>

          <div className="form-actions" style={{ marginTop: '24px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
            <button type="button" className="btn btn-secondary" onClick={() => navigate('/applications')}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              <Save size={18} />
              {saving ? 'Saving...' : 'Save Application'}
            </button>
          </div>
        </form>
      </div>

      {secretModal && (
        <SecretDisplayModal 
          clientId={secretModal.clientId}
          clientSecret={secretModal.clientSecret}
          onClose={() => navigate('/applications')}
        />
      )}
    </div>
  );
}
