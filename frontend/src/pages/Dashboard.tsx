import { useEffect, useState } from 'react';
import { Users, UserX, UserCheck, ShieldAlert, Activity, AppWindow, Key } from 'lucide-react';
import { apiClient } from '../api/client';
import './Dashboard.css';

interface DashboardMetrics {
  totalUsers: number;
  activeUsers: number;
  lockedAccounts: number;
  activeSessions: number;
  registeredApplications: number;
  failedLogins: number;
  recentSecurityEvents: number;
}

export default function Dashboard() {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchMetrics = async () => {
      try {
        const response = await apiClient.get<DashboardMetrics>('/api/v1/dashboard/metrics');
        setMetrics(response.data);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to fetch metrics');
      } finally {
        setLoading(false);
      }
    };

    fetchMetrics();
  }, []);

  if (loading) {
    return (
      <div className="dashboard-container">
        <h1 className="dashboard-title">Platform Overview</h1>
        <div className="dashboard-loading" style={{ marginTop: '2rem' }}>Loading metrics...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-container">
        <h1 className="dashboard-title">Platform Overview</h1>
        <div className="dashboard-error" style={{ marginTop: '2rem' }}>Error: {error}</div>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <h1 className="dashboard-title">Platform Overview</h1>
      <div className="metrics-grid">
        <MetricCard 
          title="Total Users" 
          value={metrics?.totalUsers ?? 0} 
          icon={<Users size={24} />} 
          color="#4F46E5"
        />
        <MetricCard 
          title="Active Users" 
          value={metrics?.activeUsers ?? 0} 
          icon={<UserCheck size={24} />} 
          color="#10B981"
        />
        <MetricCard 
          title="Locked Accounts" 
          value={metrics?.lockedAccounts ?? 0} 
          icon={<UserX size={24} />} 
          color="#EF4444"
        />
        <MetricCard 
          title="Active Sessions" 
          value={metrics?.activeSessions ?? 0} 
          icon={<Activity size={24} />} 
          color="#8B5CF6"
        />
        <MetricCard 
          title="Applications" 
          value={metrics?.registeredApplications ?? 0} 
          icon={<AppWindow size={24} />} 
          color="#F59E0B"
        />
        <MetricCard 
          title="Failed Logins" 
          value={metrics?.failedLogins ?? 0} 
          icon={<Key size={24} />} 
          color="#F97316"
        />
        <MetricCard 
          title="Security Events" 
          value={metrics?.recentSecurityEvents ?? 0} 
          icon={<ShieldAlert size={24} />} 
          color="#E11D48"
        />
      </div>
    </div>
  );
}

function MetricCard({ title, value, icon, color }: { title: string, value: number, icon: React.ReactNode, color: string }) {
  return (
    <div className="metric-card" style={{ '--accent-color': color } as React.CSSProperties}>
      <div className="metric-icon" style={{ color: color, backgroundColor: `${color}20` }}>
        {icon}
      </div>
      <div className="metric-content">
        <h3 className="metric-title">{title}</h3>
        <p className="metric-value">{value.toLocaleString()}</p>
      </div>
    </div>
  );
}
