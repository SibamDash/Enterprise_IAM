package com.enterprise.iam.repository;

import com.enterprise.iam.domain.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    Optional<Group> findByIdAndOrganizationId(UUID id, UUID organizationId);
    
    Page<Group> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    
    boolean existsByNameAndOrganizationId(String name, UUID organizationId);
}
