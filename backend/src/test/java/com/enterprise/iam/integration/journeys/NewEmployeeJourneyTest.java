package com.enterprise.iam.integration.journeys;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.Role;
import com.enterprise.iam.dto.CreateUserRequest;
import com.enterprise.iam.dto.AssignRolesRequest;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.RoleRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.JwtTokenProvider;
import com.enterprise.iam.security.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NewEmployeeJourneyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UUID tenantId;
    private String adminToken;
    private Role defaultRole;

    @BeforeEach
    void setUp() {
        Organization org = new Organization();
        org.setName("Journey Test Org");
        org = organizationRepository.save(org);
        tenantId = org.getId();

        defaultRole = new Role();
        defaultRole.setOrganizationId(tenantId);
        defaultRole.setName("USER_ROLE");
        defaultRole.setPermissions(Set.of("RESOURCE_READ"));
        defaultRole = roleRepository.save(defaultRole);

        // Create admin token
        adminToken = jwtTokenProvider.generateToken(
                UUID.randomUUID(), 
                tenantId, 
                "admin@example.com", 
                Set.of("USER_CREATE", "USER_UPDATE")
        );
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void executeNewEmployeeJourney() throws Exception {
        // 1. Admin creates user
        CreateUserRequest createReq = new CreateUserRequest();
        createReq.setEmail("newemployee@example.com");
        createReq.setFirstName("New");
        createReq.setLastName("Employee");

        String userResponse = mockMvc.perform(post("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(userResponse).get("id").asText();

        // 2. Assign Role
        AssignRolesRequest assignReq = new AssignRolesRequest();
        assignReq.setRoleIds(Set.of(defaultRole.getId()));

        mockMvc.perform(put("/api/v1/users/" + userId + "/roles")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // Note: Password creation and MFA enrollment are tested in AuthIntegrationTest / MfaControllerTest
        // This validates the admin setup part of Journey A.
    }
}
