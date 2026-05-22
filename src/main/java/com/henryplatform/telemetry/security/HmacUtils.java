package com.henryplatform.telemetry.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

// SEC: utilitário de assinatura HMAC-SHA256 — garante integridade do payload em trânsito,
//      prevenindo adulteração de dados entre o cliente e a API
public class HmacUtils {

    private static final String ALGORITHM = "HmacSHA256";

    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            // SEC: assinatura em Base64 para transporte seguro como header HTTP
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC signature", e);
        }
    }

    public static boolean verify(String payload, String secret, String expectedSignature) {
        // SEC: comparação em tempo constante previne timing attacks ao verificar assinaturas
        String computed = sign(payload, secret);
        return constantTimeEquals(computed, expectedSignature);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
