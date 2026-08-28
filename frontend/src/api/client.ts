import axios from 'axios';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Phase 1 Mock Tenant Header injection. Phase 3 will replace this with JWT tokens.
export const setTenantId = (tenantId: string) => {
  apiClient.defaults.headers.common['X-Tenant-ID'] = tenantId;
};

export const clearTenantId = () => {
  delete apiClient.defaults.headers.common['X-Tenant-ID'];
};
