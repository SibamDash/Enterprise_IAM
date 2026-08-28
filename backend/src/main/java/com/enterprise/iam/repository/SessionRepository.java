package com.enterprise.iam.repository;

import com.enterprise.iam.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByRefreshTokenHashAndTenantId(String refreshTokenHash, UUID tenantId);

    List<Session> findByUserIdAndTenantIdAndRevokedFalse(UUID userId, UUID tenantId);

    @Modifying
    @Query("UPDATE Session s SET s.revoked = true WHERE s.tokenFamily = :tokenFamily AND s.tenantId = :tenantId")
    void revokeTokenFamily(@Param("tokenFamily") UUID tokenFamily, @Param("tenantId") UUID tenantId);

    @Modifying
    @Query("UPDATE Session s SET s.revoked = true WHERE s.userId = :userId AND s.tenantId = :tenantId")
    void revokeAllUserSessions(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    @Modifying
    @Query("UPDATE Session s SET s.revoked = true WHERE s.userId = :userId AND s.id <> :exceptSessionId AND s.tenantId = :tenantId")
    void revokeOtherUserSessions(@Param("userId") UUID userId, @Param("exceptSessionId") UUID exceptSessionId, @Param("tenantId") UUID tenantId);
}
