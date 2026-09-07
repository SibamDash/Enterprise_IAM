package com.enterprise.iam.repository;

import com.enterprise.iam.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    Page<AuditLog> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
    
    Page<AuditLog> findAllByOrganizationIdAndUserIdOrderByCreatedAtDesc(UUID organizationId, UUID userId, Pageable pageable);
    
    Page<AuditLog> findAllByOrganizationIdAndEventTypeOrderByCreatedAtDesc(UUID organizationId, String eventType, Pageable pageable);
}
