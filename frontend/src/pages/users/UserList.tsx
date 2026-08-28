import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { Link } from 'react-router-dom';
import { apiClient } from '../../api/client';

export default function UserList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/users');
      return res.data;
    }
  });

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Users</h1>
        <Link to="/users/new" className="btn btn-primary">
          <Plus size={20} />
          Create User
        </Link>
      </div>

      {error && (
        <div style={{ padding: '16px', backgroundColor: '#FEF2F2', color: '#991B1B', borderRadius: '8px', marginBottom: '24px' }}>
          Error loading users. Ensure a Tenant ID is provided in the top bar.
        </div>
      )}

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={4} style={{ textAlign: 'center' }}>Loading...</td></tr>
            ) : data?.content?.length === 0 ? (
              <tr><td colSpan={4} style={{ textAlign: 'center' }}>No users found in this organization.</td></tr>
            ) : (
              data?.content?.map((user: any) => (
                <tr key={user.id}>
                  <td style={{ fontWeight: 500 }}>{user.firstName} {user.lastName}</td>
                  <td style={{ color: 'var(--text-secondary)' }}>{user.email}</td>
                  <td>
                    <span className={`badge ${user.status === 'ACTIVE' ? 'badge-active' : 'badge-inactive'}`}>
                      {user.status}
                    </span>
                  </td>
                  <td>{new Date(user.createdAt).toLocaleDateString()}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
