package com.enterprise.iam.service;

import com.enterprise.iam.domain.UserRecoveryCode;
import com.enterprise.iam.repository.UserRecoveryCodeRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;
    private final UserRecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String getQrCodeDataUri(String secret, String email) throws QrGenerationException {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("Enterprise IAM")
                .algorithm(dev.samstevens.totp.code.HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        
        byte[] imageData = qrGenerator.generate(data);
        String mimeType = qrGenerator.getImageMimeType();
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageData);
    }

    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    @Transactional
    public List<String> generateRecoveryCodes(UUID userId) {
        recoveryCodeRepository.deleteByUserId(userId);
        List<String> plainCodes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < 10; i++) {
            // Generate a 10 character alphanumeric code
            byte[] bytes = new byte[8];
            random.nextBytes(bytes);
            String plainCode = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 10);
            plainCodes.add(plainCode);

            UserRecoveryCode recoveryCode = new UserRecoveryCode();
            recoveryCode.setUserId(userId);
            recoveryCode.setCodeHash(passwordEncoder.encode(plainCode));
            recoveryCodeRepository.save(recoveryCode);
        }
        return plainCodes;
    }

    @Transactional
    public boolean verifyAndConsumeRecoveryCode(UUID userId, String code) {
        List<UserRecoveryCode> codes = recoveryCodeRepository.findByUserId(userId);
        for (UserRecoveryCode recoveryCode : codes) {
            if (!recoveryCode.isUsed() && passwordEncoder.matches(code, recoveryCode.getCodeHash())) {
                recoveryCode.setUsed(true);
                recoveryCode.setUsedAt(Instant.now());
                recoveryCodeRepository.save(recoveryCode);
                return true;
            }
        }
        return false;
    }
}
