import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { Link } from 'react-router-dom';
import { apiClient } from '../../api/client';

export default function OrganizationList() {
  const { data, isLoading } = useQuery({
    queryKey: ['organizations'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/organizations');
      return res.data;
    }
  });

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Organizations</h1>
        <Link to="/organizations/new" className="btn btn-primary">
          <Plus size={20} />
          Create Organization
        </Link>
      </div>

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={4} style={{ textAlign: 'center' }}>Loading...</td></tr>
            ) : data?.content?.length === 0 ? (
              <tr><td colSpan={4} style={{ textAlign: 'center' }}>No organizations found.</td></tr>
            ) : (
              data?.content?.map((org: any) => (
                <tr key={org.id}>
                  <td style={{ fontFamily: 'monospace', fontSize: '0.875rem' }}>{org.id}</td>
                  <td style={{ fontWeight: 500 }}>{org.name}</td>
                  <td>
                    <span className={`badge ${org.status === 'ACTIVE' ? 'badge-active' : 'badge-inactive'}`}>
                      {org.status}
                    </span>
                  </td>
                  <td>{new Date(org.createdAt).toLocaleDateString()}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
