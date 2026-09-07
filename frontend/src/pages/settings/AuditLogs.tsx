import { useQuery } from '@tanstack/react-query';
import { Activity } from 'lucide-react';
import { apiClient } from '../../api/client';

export default function AuditLogs() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['audit-logs'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/audit-logs');
      return res.data;
    }
  });

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">
          <Activity size={24} style={{ marginRight: '12px', color: 'var(--primary-color)' }} />
          Audit Logs
        </h1>
      </div>

      {error && (
        <div style={{ padding: '16px', backgroundColor: '#FEF2F2', color: '#991B1B', borderRadius: '8px', marginBottom: '24px' }}>
          Error loading audit logs. Ensure you have the AUDIT_READ permission and a Tenant ID is provided.
        </div>
      )}

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>Event Type</th>
              <th>User ID</th>
              <th>IP Address</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={5} style={{ textAlign: 'center' }}>Loading...</td></tr>
            ) : data?.content?.length === 0 ? (
              <tr><td colSpan={5} style={{ textAlign: 'center' }}>No audit logs found.</td></tr>
            ) : (
              data?.content?.map((log: any) => (
                <tr key={log.id}>
                  <td style={{ whiteSpace: 'nowrap', color: 'var(--text-secondary)' }}>
                    {new Date(log.createdAt).toLocaleString()}
                  </td>
                  <td>
                    <span className="badge badge-active" style={{ backgroundColor: '#EEF2FF', color: '#4F46E5' }}>
                      {log.eventType}
                    </span>
                  </td>
                  <td style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                    {log.userId || '-'}
                  </td>
                  <td style={{ fontSize: '13px' }}>{log.ipAddress || '-'}</td>
                  <td style={{ fontSize: '13px', maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {log.details}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
