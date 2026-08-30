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
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() == 0) {
            log.info("Database is empty. Seeding initial data...");

            Organization org = new Organization();
            org.setName("Acme Corp");
            org.setStatus("ACTIVE");
            org = organizationRepository.save(org);

            Role superAdminRole = new Role();
            superAdminRole.setOrganizationId(org.getId());
            superAdminRole.setName("SUPER_ADMIN");
            superAdminRole.setDescription("Super Administrator Role");
            superAdminRole.setPermissions(new HashSet<>(Arrays.asList(
                    "USER_CREATE", "USER_READ", "USER_UPDATE", "USER_DELETE",
                    "ROLE_CREATE", "ROLE_READ", "ROLE_UPDATE", "ROLE_DELETE",
                    "ORG_CREATE", "ORG_READ", "ORG_UPDATE", "ORG_DELETE",
                    "GROUP_CREATE", "GROUP_READ", "GROUP_UPDATE", "GROUP_DELETE",
                    "POLICY_CREATE", "POLICY_READ", "POLICY_UPDATE", "POLICY_DELETE"
            )));
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
