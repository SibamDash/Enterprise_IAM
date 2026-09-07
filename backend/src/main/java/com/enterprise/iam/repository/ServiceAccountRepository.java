package com.enterprise.iam.repository;

import com.enterprise.iam.domain.ServiceAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceAccountRepository extends JpaRepository<ServiceAccount, UUID> {
    
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<ServiceAccount> findByClientId(String clientId);
    
    @EntityGraph(attributePaths = {"roles"})
    Optional<ServiceAccount> findByIdAndOrganizationId(UUID id, UUID organizationId);
    
    List<ServiceAccount> findAllByOrganizationId(UUID organizationId);
}
