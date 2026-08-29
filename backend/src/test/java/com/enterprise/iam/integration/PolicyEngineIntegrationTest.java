package com.enterprise.iam.integration;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.Policy;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.PolicyRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.PolicyEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.enterprise.iam.TestcontainersConfiguration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public class PolicyEngineIntegrationTest {

    @Autowired
    private PolicyEngine policyEngine;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization org;
    private User testUser;

    @BeforeEach
    void setUp() {
        org = new Organization();
        org.setName("Policy Test Org");
        org = organizationRepository.save(org);

        testUser = new User();
        testUser.setOrganizationId(org.getId());
        testUser.setEmail("policyuser@example.com");
        testUser.setFirstName("Policy");
        testUser.setLastName("User");
        testUser.setStatus("ACTIVE");
        testUser.getAttributes().put("department", "Finance");
        testUser.getAttributes().put("clearance", "secret");
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        policyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void testBasicAllowPolicy() {
        Policy allowPolicy = new Policy();
        allowPolicy.setOrganizationId(org.getId());
        allowPolicy.setName("Allow Read Reports");
        allowPolicy.setEffect("ALLOW");
        allowPolicy.setActions(List.of("READ"));
        allowPolicy.setResources(List.of("report:*"));
        policyRepository.save(allowPolicy);

        boolean result = policyEngine.evaluate(testUser, "READ", "report:finance", Map.of());
        assertTrue(result, "Should allow READ on report:finance due to report:* matching");

        boolean wrongAction = policyEngine.evaluate(testUser, "WRITE", "report:finance", Map.of());
        assertFalse(wrongAction, "Should deny WRITE on report:finance");
    }

    @Test
    void testExplicitDenyOverridesAllow() {
        Policy allowPolicy = new Policy();
        allowPolicy.setOrganizationId(org.getId());
        allowPolicy.setName("Allow All Reports");
        allowPolicy.setEffect("ALLOW");
        allowPolicy.setActions(List.of("READ"));
        allowPolicy.setResources(List.of("report:*"));
        allowPolicy.setPriority(10);
        policyRepository.save(allowPolicy);

        Policy denyPolicy = new Policy();
        denyPolicy.setOrganizationId(org.getId());
        denyPolicy.setName("Deny Secret Reports");
        denyPolicy.setEffect("DENY");
        denyPolicy.setActions(List.of("READ"));
        denyPolicy.setResources(List.of("report:secret"));
        denyPolicy.setPriority(20);
        policyRepository.save(denyPolicy);

        boolean allowResult = policyEngine.evaluate(testUser, "READ", "report:public", Map.of());
        assertTrue(allowResult, "Should allow READ on report:public");

        boolean denyResult = policyEngine.evaluate(testUser, "READ", "report:secret", Map.of());
        assertFalse(denyResult, "Should deny READ on report:secret due to explicit DENY");
    }

    @Test
    void testSpelConditionEvaluation() {
        Policy condPolicy = new Policy();
        condPolicy.setOrganizationId(org.getId());
        condPolicy.setName("Department Matching Policy");
        condPolicy.setEffect("ALLOW");
        condPolicy.setActions(List.of("READ"));
        condPolicy.setResources(List.of("document:*"));
        // Condition: User's department must equal Resource's department
        condPolicy.setConditions("#user.attributes['department'] == #resource['department']");
        policyRepository.save(condPolicy);

        Map<String, Object> financeResource = Map.of("department", "Finance");
        boolean allowResult = policyEngine.evaluate(testUser, "READ", "document:ledger", financeResource);
        assertTrue(allowResult, "Should allow since user department is Finance and resource department is Finance");

        Map<String, Object> hrResource = Map.of("department", "HR");
        boolean denyResult = policyEngine.evaluate(testUser, "READ", "document:ledger", hrResource);
        assertFalse(denyResult, "Should deny since user department is Finance but resource department is HR");
    }
}
