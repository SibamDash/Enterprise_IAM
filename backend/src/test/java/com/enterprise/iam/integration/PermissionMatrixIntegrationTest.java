package com.enterprise.iam.integration;


import com.enterprise.iam.domain.Group;
import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.Role;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.repository.GroupRepository;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.RoleRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")

public class PermissionMatrixIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UUID tenantId;
    private User testUser;
    private Role adminRole;
    private Group adminGroup;

    @BeforeEach
    void setUp() {
        Organization org = new Organization();
        org.setName("RBAC Test Org");
        org = organizationRepository.save(org);
        tenantId = org.getId();

        testUser = new User();
        testUser.setOrganizationId(tenantId);
        testUser.setEmail("user@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setStatus("ACTIVE");
        testUser = userRepository.save(testUser);

        adminRole = new Role();
        adminRole.setOrganizationId(tenantId);
        adminRole.setName("Admin");
        adminRole.setPermissions(Set.of("ROLE_CREATE", "USER_READ"));
        adminRole = roleRepository.save(adminRole);
        
        adminGroup = new Group();
        adminGroup.setOrganizationId(tenantId);
        adminGroup.setName("Admins");
        adminGroup = groupRepository.save(adminGroup);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        groupRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void accessDeniedWhenMissingPermission() throws Exception {
        String token = jwtTokenProvider.generateToken(testUser.getId(), tenantId, testUser.getEmail());
        
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void accessGrantedWithDirectRole() throws Exception {
        testUser.setRoles(Set.of(adminRole));
        userRepository.save(testUser);

        String token = jwtTokenProvider.generateToken(testUser.getId(), tenantId, testUser.getEmail());
        
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void accessGrantedWithGroupDerivedRole() throws Exception {
        adminGroup.setRoles(Set.of(adminRole));
        groupRepository.save(adminGroup);
        
        testUser.setGroups(Set.of(adminGroup));
        userRepository.save(testUser);

        String token = jwtTokenProvider.generateToken(testUser.getId(), tenantId, testUser.getEmail());
        
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void instantRevocationRemovesAccess() throws Exception {
        testUser.setRoles(Set.of(adminRole));
        userRepository.save(testUser);

        String token = jwtTokenProvider.generateToken(testUser.getId(), tenantId, testUser.getEmail());
        
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
                
        // Revoke role
        testUser.getRoles().clear();
        userRepository.save(testUser);
        
        // Same token, but should now be forbidden
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
