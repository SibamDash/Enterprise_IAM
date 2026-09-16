package com.enterprise.iam.bootstrap;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.Role;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.RoleRepository;
import com.enterprise.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() == 0) {
            log.info("Database is empty. Seeding initial data...");

            java.util.UUID orgId = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
            entityManager.createNativeQuery(
                "INSERT INTO organizations (id, name, status, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter(1, orgId)
                .setParameter(2, "Acme Corp")
                .setParameter(3, "ACTIVE")
                .executeUpdate();

            Organization org = entityManager.find(Organization.class, orgId);

            Role superAdminRole = new Role();
            superAdminRole.setOrganizationId(org.getId());
            superAdminRole.setName("SUPER_ADMIN");
            superAdminRole.setDescription("Super Administrator Role");
            superAdminRole.setPermissions(new HashSet<>(Arrays.asList(
                    "USER_CREATE", "USER_READ", "USER_UPDATE", "USER_DELETE",
                    "ROLE_CREATE", "ROLE_READ", "ROLE_UPDATE", "ROLE_DELETE",
                    "ORG_CREATE", "ORG_READ", "ORG_UPDATE", "ORG_DELETE",
                    "GROUP_CREATE", "GROUP_READ", "GROUP_UPDATE", "GROUP_DELETE",
                    "POLICY_CREATE", "POLICY_READ", "POLICY_UPDATE", "POLICY_DELETE",
                    "CLIENT_CREATE", "CLIENT_READ", "CLIENT_UPDATE", "CLIENT_DELETE",
                    "AUDIT_READ")));
            superAdminRole = roleRepository.save(superAdminRole);

            User admin = new User();
            admin.setOrganizationId(org.getId());
            admin.setEmail("admin@acme.com");
            admin.setFirstName("Super");
            admin.setLastName("Admin");
            admin.setPasswordHash(passwordEncoder.encode("SecurePassword123!"));
            admin.setStatus("ACTIVE");
            admin.getRoles().add(superAdminRole);
            userRepository.save(admin);

            log.info("Seeding completed. Super admin created: admin@acme.com / SecurePassword123!");
        } else {
            log.info("Database already seeded.");
        }
    }
}