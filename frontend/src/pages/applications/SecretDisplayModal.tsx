import { useState } from 'react';
import { Copy, Check, AlertTriangle } from 'lucide-react';

interface SecretDisplayModalProps {
  clientId: string;
  clientSecret: string;
  onClose: () => void;
}

export default function SecretDisplayModal({ clientId, clientSecret, onClose }: SecretDisplayModalProps) {
  const [copiedId, setCopiedId] = useState(false);
  const [copiedSecret, setCopiedSecret] = useState(false);

  const copyToClipboard = (text: string, isSecret: boolean) => {
    navigator.clipboard.writeText(text);
    if (isSecret) {
      setCopiedSecret(true);
      setTimeout(() => setCopiedSecret(false), 2000);
    } else {
      setCopiedId(true);
      setTimeout(() => setCopiedId(false), 2000);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal" style={{ maxWidth: '600px' }}>
        <div className="modal-header">
          <h3>Application Credentials</h3>
        </div>
        
        <div className="modal-body">
          <div style={{ backgroundColor: '#fffbeb', color: '#b45309', padding: '16px', borderRadius: '6px', marginBottom: '24px', display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
            <AlertTriangle size={24} style={{ flexShrink: 0 }} />
            <div>
              <p style={{ margin: '0 0 8px 0', fontWeight: 'bold' }}>Important: Save your client secret</p>
              <p style={{ margin: 0, fontSize: '14px' }}>
                This is the <strong>only time</strong> the client secret will be shown. If you lose it, you will need to generate a new one.
              </p>
            </div>
          </div>

          <div className="form-group">
            <label>Client ID</label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <input type="text" className="form-control" value={clientId} readOnly />
              <button 
                className="btn btn-secondary"
                onClick={() => copyToClipboard(clientId, false)}
                title="Copy Client ID"
              >
                {copiedId ? <Check size={18} /> : <Copy size={18} />}
              </button>
            </div>
          </div>

          <div className="form-group" style={{ marginTop: '16px' }}>
            <label>Client Secret</label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <input type="text" className="form-control" value={clientSecret} readOnly style={{ fontFamily: 'monospace' }} />
              <button 
                className="btn btn-secondary"
                onClick={() => copyToClipboard(clientSecret, true)}
                title="Copy Client Secret"
              >
                {copiedSecret ? <Check size={18} /> : <Copy size={18} />}
              </button>
            </div>
          </div>
        </div>

        <div className="modal-footer">
          <button className="btn btn-primary" onClick={onClose}>
            I have saved the credentials
          </button>
        </div>
      </div>
    </div>
  );
}
