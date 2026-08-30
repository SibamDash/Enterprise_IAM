package com.enterprise.iam.repository;

import com.enterprise.iam.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId);
    
    Optional<User> findByEmailAndOrganizationId(String email, UUID organizationId);
    
    Page<User> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    
    boolean existsByEmailAndOrganizationId(String email, UUID organizationId);

    @EntityGraph(attributePaths = {"roles", "groups", "groups.roles"})
    Optional<User> findWithRolesById(UUID id);

    @EntityGraph(attributePaths = {"roles", "groups", "groups.roles"})
    Optional<User> findWithRolesByEmailAndOrganizationId(String email, UUID organizationId);
}
