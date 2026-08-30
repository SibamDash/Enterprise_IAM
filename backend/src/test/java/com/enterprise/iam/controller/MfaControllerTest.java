package com.enterprise.iam.controller;

import com.enterprise.iam.domain.User;
import com.enterprise.iam.dto.MfaDisableRequest;
import com.enterprise.iam.dto.MfaEnableRequest;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.JwtAuthenticationFilter;
import com.enterprise.iam.service.MfaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = MfaController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class MfaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MfaService mfaService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setMfaEnabled(false);
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testSetup_Success() throws Exception {
        UUID mockUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        testUser.setId(mockUserId);
        when(userRepository.findById(mockUserId)).thenReturn(Optional.of(testUser));
        when(mfaService.generateSecret()).thenReturn("SECRET");
        when(mfaService.getQrCodeDataUri(eq("SECRET"), anyString())).thenReturn("data:image/png;base64,123");

        mockMvc.perform(post("/api/v1/mfa/setup").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("SECRET"))
                .andExpect(jsonPath("$.qrCodeUri").value("data:image/png;base64,123"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testEnable_Success() throws Exception {
        UUID mockUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        testUser.setId(mockUserId);
        when(userRepository.findById(mockUserId)).thenReturn(Optional.of(testUser));
        when(mfaService.verifyCode("SECRET", "123456")).thenReturn(true);
        when(mfaService.generateRecoveryCodes(mockUserId)).thenReturn(List.of("code1", "code2"));

        MfaEnableRequest request = new MfaEnableRequest();
        request.setSecret("SECRET");
        request.setCode("123456");

        mockMvc.perform(post("/api/v1/mfa/enable")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryCodes[0]").value("code1"));
        
        verify(userRepository, times(1)).save(testUser);
        assert(testUser.isMfaEnabled());
    }
}
