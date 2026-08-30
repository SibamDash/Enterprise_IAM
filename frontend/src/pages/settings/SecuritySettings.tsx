import { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { apiClient } from '../../api/client';

export default function SecuritySettings() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  // Setup state
  const [setupData, setSetupData] = useState<{ secret: string; qrCodeUri: string } | null>(null);
  const [mfaCode, setMfaCode] = useState('');
  
  // Result state
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [mfaEnabled, setMfaEnabled] = useState(false); // In a real app, fetch from a /me endpoint

  const handleSetup = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await apiClient.post('/api/v1/mfa/setup');
      setSetupData(res.data);
    } catch (err: any) {
      setError(err.response?.status === 409 ? 'MFA is already enabled' : 'Failed to start MFA setup');
    } finally {
      setLoading(false);
    }
  };

  const handleEnable = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!setupData) return;
    
    setLoading(true);
    setError('');
    try {
      const res = await apiClient.post('/api/v1/mfa/enable', {
        secret: setupData.secret,
        code: mfaCode
      });
      setRecoveryCodes(res.data.recoveryCodes);
      setMfaEnabled(true);
      setSetupData(null);
    } catch (err: any) {
      setError('Invalid code. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleDisable = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await apiClient.post('/api/v1/mfa/disable', { code: mfaCode });
      setMfaEnabled(false);
      setMfaCode('');
      setRecoveryCodes(null);
    } catch (err: any) {
      setError('Invalid code. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card">
      <div className="card-header">
        <h3 className="card-title">Security Settings</h3>
      </div>
      <div className="card-body">
        {error && (
          <div style={{ padding: '12px', backgroundColor: 'var(--danger-color)', color: 'white', borderRadius: 'var(--border-radius)', marginBottom: '16px', fontSize: '0.875rem' }}>
            {error}
          </div>
        )}

        {recoveryCodes ? (
          <div>
            <h4 style={{ color: 'var(--success-color)', marginBottom: '16px' }}>MFA Enabled Successfully!</h4>
            <p style={{ marginBottom: '16px', color: 'var(--text-secondary)' }}>
              Save these recovery codes in a safe place. You will need them if you lose access to your authenticator app.
              Each code can only be used once.
            </p>
            <div style={{ backgroundColor: 'var(--bg-secondary)', padding: '16px', borderRadius: 'var(--border-radius)', fontFamily: 'monospace', fontSize: '1rem', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
              {recoveryCodes.map(c => <div key={c}>{c}</div>)}
            </div>
            <button className="btn btn-primary" onClick={() => setRecoveryCodes(null)} style={{ marginTop: '24px' }}>
              I have saved my codes
            </button>
          </div>
        ) : setupData ? (
          <form onSubmit={handleEnable}>
            <h4 style={{ marginBottom: '16px' }}>Setup Authenticator App</h4>
            <p style={{ marginBottom: '16px', color: 'var(--text-secondary)' }}>
              Scan this QR code with your authenticator app (e.g., Google Authenticator, Authy).
            </p>
            <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '24px', backgroundColor: 'white', padding: '16px', borderRadius: '8px', display: 'inline-block' }}>
              <QRCodeSVG value={setupData.qrCodeUri} size={200} />
            </div>
            <div className="form-group" style={{ maxWidth: '300px' }}>
              <label className="form-label">Secret Key</label>
              <input type="text" className="form-control" value={setupData.secret} readOnly />
            </div>
            <div className="form-group" style={{ maxWidth: '300px' }}>
              <label className="form-label" htmlFor="mfaCode">Enter 6-digit code</label>
              <input
                id="mfaCode"
                type="text"
                className="form-control"
                value={mfaCode}
                onChange={(e) => setMfaCode(e.target.value)}
                required
                maxLength={6}
                autoComplete="off"
              />
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Verifying...' : 'Verify and Enable'}
              </button>
              <button type="button" className="btn btn-outline" onClick={() => setSetupData(null)}>
                Cancel
              </button>
            </div>
          </form>
        ) : (
          <div>
            <p style={{ marginBottom: '24px', color: 'var(--text-secondary)' }}>
              Multi-factor authentication adds an extra layer of security to your account.
              {mfaEnabled ? ' MFA is currently enabled.' : ''}
            </p>
            
            {mfaEnabled ? (
              <form onSubmit={handleDisable} style={{ maxWidth: '300px' }}>
                <div className="form-group">
                  <label className="form-label" htmlFor="disableCode">Enter code to disable MFA</label>
                  <input
                    id="disableCode"
                    type="text"
                    className="form-control"
                    value={mfaCode}
                    onChange={(e) => setMfaCode(e.target.value)}
                    required
                    maxLength={6}
                    autoComplete="off"
                  />
                </div>
                <button type="submit" className="btn btn-danger" disabled={loading}>
                  {loading ? 'Disabling...' : 'Disable MFA'}
                </button>
              </form>
            ) : (
              <button className="btn btn-primary" onClick={handleSetup} disabled={loading}>
                {loading ? 'Loading...' : 'Set up MFA'}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
