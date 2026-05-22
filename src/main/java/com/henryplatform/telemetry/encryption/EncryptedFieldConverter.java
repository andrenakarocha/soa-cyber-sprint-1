package com.henryplatform.telemetry.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

// SEC: campos sensíveis (padrão de degradação de óleo, temperatura — revelam hábitos de direção)
//      são criptografados antes de persistir no banco, garantindo proteção mesmo se o banco for comprometido
@Converter
@Component
public class EncryptedFieldConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES";
    private final SecretKeySpec keySpec;

    public EncryptedFieldConverter(@Value("${security.encryption.key}") String key) {
        // SEC: chave AES derivada de configuração externa — nunca embutida no código
        byte[] keyBytes = key.substring(0, 16).getBytes();
        this.keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            // SEC: dado cifrado + Base64 para armazenamento seguro como texto no banco
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt field", e);
        }
    }
}
