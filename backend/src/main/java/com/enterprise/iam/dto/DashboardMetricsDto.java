package com.enterprise.iam.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardMetricsDto {
    private long totalUsers;
    private long activeUsers;
    private long lockedAccounts;
    private long activeSessions;
    private long registeredApplications;
    private long failedLogins;
    private long recentSecurityEvents;
}
