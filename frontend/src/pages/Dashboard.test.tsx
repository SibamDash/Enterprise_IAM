import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import Dashboard from './Dashboard';
import { apiClient } from '../api/client';
import { MemoryRouter } from 'react-router-dom';

vi.mock('../api/client', () => ({
  apiClient: {
    get: vi.fn(),
  },
}));

describe('Dashboard Component', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('renders loading state initially', () => {
    (apiClient.get as any).mockImplementation(() => new Promise(() => {})); // Never resolves
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );
    expect(screen.getByText('Loading metrics...')).toBeInTheDocument();
  });

  it('renders error state when API call fails', async () => {
    (apiClient.get as any).mockRejectedValueOnce(new Error('Network error'));
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Failed to fetch metrics/i)).toBeInTheDocument();
    });
  });

  it('renders metrics when API call succeeds', async () => {
    const mockMetrics = {
      totalUsers: 150,
      activeUsers: 145,
      lockedAccounts: 5,
      activeSessions: 42,
      registeredApplications: 10,
      failedLogins: 3,
      recentSecurityEvents: 1,
    };

    (apiClient.get as any).mockResolvedValueOnce({ data: mockMetrics });

    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Platform Overview')).toBeInTheDocument();
      expect(screen.getByText('150')).toBeInTheDocument();
      expect(screen.getByText('145')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('42')).toBeInTheDocument();
      expect(screen.getByText('10')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument();
    });
  });
});
