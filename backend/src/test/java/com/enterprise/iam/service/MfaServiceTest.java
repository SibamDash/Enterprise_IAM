package com.enterprise.iam.service;

import com.enterprise.iam.domain.UserRecoveryCode;
import com.enterprise.iam.repository.UserRecoveryCodeRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MfaServiceTest {

    @Mock
    private SecretGenerator secretGenerator;
    @Mock
    private QrGenerator qrGenerator;
    @Mock
    private CodeVerifier codeVerifier;
    @Mock
    private UserRecoveryCodeRepository recoveryCodeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MfaService mfaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateSecret() {
        when(secretGenerator.generate()).thenReturn("SECRET123");
        String secret = mfaService.generateSecret();
        assertEquals("SECRET123", secret);
    }

    @Test
    void testVerifyCode_Success() {
        when(codeVerifier.isValidCode("SECRET123", "123456")).thenReturn(true);
        assertTrue(mfaService.verifyCode("SECRET123", "123456"));
    }

    @Test
    void testVerifyCode_Failure() {
        when(codeVerifier.isValidCode("SECRET123", "000000")).thenReturn(false);
        assertFalse(mfaService.verifyCode("SECRET123", "000000"));
    }

    @Test
    void testGenerateRecoveryCodes() {
        UUID userId = UUID.randomUUID();
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed_code");

        List<String> codes = mfaService.generateRecoveryCodes(userId);

        assertEquals(10, codes.size());
        verify(recoveryCodeRepository, times(1)).deleteByUserId(userId);
        verify(recoveryCodeRepository, times(10)).save(any(UserRecoveryCode.class));
    }

    @Test
    void testVerifyAndConsumeRecoveryCode_Success() {
        UUID userId = UUID.randomUUID();
        UserRecoveryCode recoveryCode = new UserRecoveryCode();
        recoveryCode.setUserId(userId);
        recoveryCode.setCodeHash("hashed_code");
        recoveryCode.setUsed(false);

        when(recoveryCodeRepository.findByUserId(userId)).thenReturn(List.of(recoveryCode));
        when(passwordEncoder.matches("plain_code", "hashed_code")).thenReturn(true);

        boolean result = mfaService.verifyAndConsumeRecoveryCode(userId, "plain_code");

        assertTrue(result);
        assertTrue(recoveryCode.isUsed());
        verify(recoveryCodeRepository, times(1)).save(recoveryCode);
    }

    @Test
    void testVerifyAndConsumeRecoveryCode_Failure_UsedCode() {
        UUID userId = UUID.randomUUID();
        UserRecoveryCode recoveryCode = new UserRecoveryCode();
        recoveryCode.setUserId(userId);
        recoveryCode.setCodeHash("hashed_code");
        recoveryCode.setUsed(true);

        when(recoveryCodeRepository.findByUserId(userId)).thenReturn(List.of(recoveryCode));

        boolean result = mfaService.verifyAndConsumeRecoveryCode(userId, "plain_code");

        assertFalse(result);
        verify(recoveryCodeRepository, never()).save(any());
    }

    @Test
    void testVerifyAndConsumeRecoveryCode_Failure_InvalidCode() {
        UUID userId = UUID.randomUUID();
        UserRecoveryCode recoveryCode = new UserRecoveryCode();
        recoveryCode.setUserId(userId);
        recoveryCode.setCodeHash("hashed_code");
        recoveryCode.setUsed(false);

        when(recoveryCodeRepository.findByUserId(userId)).thenReturn(List.of(recoveryCode));
        when(passwordEncoder.matches("wrong_code", "hashed_code")).thenReturn(false);

        boolean result = mfaService.verifyAndConsumeRecoveryCode(userId, "wrong_code");

        assertFalse(result);
        verify(recoveryCodeRepository, never()).save(any());
    }
}
