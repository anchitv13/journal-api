package com.parzival.journal.util;


import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public final class ObfuscationUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String VERSION = "v1";

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private static final SecretKey SECRET_KEY = loadKey();

    private ObfuscationUtil() {
    }

    private static SecretKey loadKey() {
        String encodedKey = System.getenv("ID_ENCRYPTION_KEY");

        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "ID_ENCRYPTION_KEY environment variable is missing"
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "ID_ENCRYPTION_KEY must be Base64 encoded", ex
            );
        }

        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "ID_ENCRYPTION_KEY must contain 32 bytes (AES-256)"
            );
        }

        return new SecretKeySpec(keyBytes, "AES");
    }

    public static String encrypt(Long value) {
        if (value == null) {
            return null;
        }

        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            SECURE_RANDOM.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    SECRET_KEY,
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce)
            );

            byte[] plaintext = ByteBuffer.allocate(Long.BYTES)
                    .putLong(value)
                    .array();

            byte[] ciphertext = cipher.doFinal(plaintext);

            ByteBuffer output = ByteBuffer.allocate(
                    nonce.length + ciphertext.length
            );
            output.put(nonce);
            output.put(ciphertext);

            return VERSION + "." + Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(output.array());

        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Unable to encrypt ID", ex
            );
        }
    }

    public static Long decrypt(String encryptedValue) {
        if (encryptedValue == null) {
            return null;
        }

        try {
            String[] parts = encryptedValue.split("\\.", -1);

            if (parts.length != 2 || !VERSION.equals(parts[0])) {
                throw new IllegalArgumentException(
                        "Invalid encrypted ID format"
                );
            }

            byte[] input = Base64.getUrlDecoder()
                    .decode(parts[1]);

            if (input.length < NONCE_LENGTH + 8 + 16) {
                throw new IllegalArgumentException(
                        "Invalid encrypted ID length"
                );
            }

            ByteBuffer buffer = ByteBuffer.wrap(input);
            byte[] nonce = new byte[NONCE_LENGTH];
            buffer.get(nonce);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    SECRET_KEY,
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce)
            );

            byte[] plaintext = cipher.doFinal(ciphertext);

            if (plaintext.length != Long.BYTES) {
                throw new IllegalArgumentException(
                        "Invalid decrypted ID"
                );
            }

            return ByteBuffer.wrap(plaintext).getLong();

        } catch (GeneralSecurityException
                 | IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid or tampered encrypted ID", ex
            );
        }
    }
}
