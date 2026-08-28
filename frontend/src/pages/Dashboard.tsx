import { Building2, Users } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Dashboard() {
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
      </div>
      
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
        <div className="card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '16px' }}>
            <Building2 size={32} color="var(--primary-color)" />
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Organizations</h3>
          </div>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
            Manage tenants, view status, and configure organizational settings.
          </p>
          <Link to="/organizations" className="btn btn-primary">
            View Organizations
          </Link>
        </div>

        <div className="card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '16px' }}>
            <Users size={32} color="var(--primary-color)" />
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Users</h3>
          </div>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
            Manage users, assign roles, and configure access within your current tenant.
          </p>
          <Link to="/users" className="btn btn-primary">
            View Users
          </Link>
        </div>
      </div>
    </div>
  );
}
