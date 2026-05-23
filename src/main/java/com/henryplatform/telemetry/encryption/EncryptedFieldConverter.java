package com.henryplatform.telemetry.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

// SEC: campos sensíveis (temperatura, vida do óleo — revelam hábitos de direção)
//      criptografados com AES-256/GCM: cifra autenticada + IV único por registro
@Converter
@Component
public class EncryptedFieldConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    // SEC: IV de 12 bytes (96 bits) — tamanho recomendado pelo NIST SP 800-38D para GCM
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptedFieldConverter(@Value("${security.encryption.key}") String key) {
        // SEC: chave AES-256 (32 bytes) derivada de configuração externa — nunca embutida no código
        byte[] keyBytes = key.substring(0, 32).getBytes();
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            // SEC: IV aleatório por registro — blocos idênticos produzem ciphertext distinto (diferente de ECB)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes());

            // SEC: formato base64(IV):base64(ciphertext+tag) — IV não é segredo, deve ser armazenado junto
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            String[] parts = dbData.split(":", 2);
            if (parts.length != 2) throw new IllegalStateException("Invalid encrypted field format");

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            // SEC: GCM verifica tag de autenticidade antes de decifrar — adulteração é detectada
            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt field", e);
        }
    }
}
