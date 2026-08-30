package com.enterprise.iam.repository;

import com.enterprise.iam.domain.UserRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRecoveryCodeRepository extends JpaRepository<UserRecoveryCode, UUID> {
    List<UserRecoveryCode> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
