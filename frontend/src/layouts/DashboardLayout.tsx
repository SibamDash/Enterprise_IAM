import { Outlet, NavLink } from 'react-router-dom';
import { LayoutDashboard, Building2, Users, Shield, UserCircle } from 'lucide-react';
import { useEffect, useState } from 'react';
import { setTenantId } from '../api/client';

export default function DashboardLayout() {
  const [tenant, setTenant] = useState(localStorage.getItem('tenantId') || '');

  // Simulate Tenant context for Phase 1
  useEffect(() => {
    if (tenant) {
      setTenantId(tenant);
      localStorage.setItem('tenantId', tenant);
    }
  }, [tenant]);

  return (
    <div className="app-container">
      <aside className="sidebar">
        <div className="sidebar-header">
          <Shield className="text-primary" size={32} color="#4F46E5" />
          <h2>Enterprise IAM</h2>
        </div>
        <nav className="sidebar-nav">
          <NavLink to="/" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <LayoutDashboard size={20} />
            Dashboard
          </NavLink>
          <NavLink to="/organizations" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <Building2 size={20} />
            Organizations
          </NavLink>
          <NavLink to="/users" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <Users size={20} />
            Users
          </NavLink>
        </nav>
      </aside>
      
      <main className="main-content">
        <header className="topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <span style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>Current Tenant:</span>
            <input 
              type="text" 
              className="form-control"
              style={{ width: '250px', margin: 0, padding: '6px 12px' }}
              placeholder="Enter Organization UUID" 
              value={tenant}
              onChange={(e) => setTenant(e.target.value)}
            />
            <UserCircle size={32} color="var(--text-secondary)" />
          </div>
        </header>
        <div className="content-area">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
