package com.enterprise.iam.repository;

import com.enterprise.iam.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByIdAndOrganizationId(UUID id, UUID organizationId);
    
    Page<Role> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    
    boolean existsByNameAndOrganizationId(String name, UUID organizationId);
}
