import { Outlet } from 'react-router-dom';
import { Shield } from 'lucide-react';

export default function AuthLayout() {
  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'center',
      backgroundColor: 'var(--background-color)',
      padding: '24px'
    }}>
      <div style={{ marginBottom: '32px', textAlign: 'center' }}>
        <Shield size={48} color="var(--primary-color)" style={{ margin: '0 auto 16px' }} />
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>Enterprise IAM</h1>
      </div>
      
      <div className="card" style={{ width: '100%', maxWidth: '400px', padding: '32px' }}>
        <Outlet />
      </div>
    </div>
  );
}
