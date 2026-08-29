package com.enterprise.iam.integration;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.context.annotation.Import;
import com.enterprise.iam.TestcontainersConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TenantIsolationIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    private Organization orgA;
    private Organization orgB;
    private User userInOrgA;

    @BeforeEach
    void setUp() {
        orgA = new Organization();
        orgA.setName("Org A");
        orgA = organizationRepository.save(orgA);

        orgB = new Organization();
        orgB.setName("Org B");
        orgB = organizationRepository.save(orgB);

        userInOrgA = new User();
        userInOrgA.setOrganizationId(orgA.getId());
        userInOrgA.setEmail("userA@orga.com");
        userInOrgA.setFirstName("Alice");
        userInOrgA.setLastName("Smith");
        userInOrgA = userRepository.save(userInOrgA);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        TenantContextHolder.clear();
    }

    @Test
    void userShouldBeVisibleInTheirOwnTenant() {
        TenantContextHolder.setTenantId(orgA.getId());

        Optional<User> foundUser = userRepository.findByIdAndOrganizationId(userInOrgA.getId(), TenantContextHolder.getTenantId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("userA@orga.com");
    }

    @Test
    void userShouldNotBeVisibleInAnotherTenant() {
        TenantContextHolder.setTenantId(orgB.getId()); // Set context to Org B

        Optional<User> foundUser = userRepository.findByIdAndOrganizationId(userInOrgA.getId(), TenantContextHolder.getTenantId());
        assertThat(foundUser).isNotPresent();
    }
}
