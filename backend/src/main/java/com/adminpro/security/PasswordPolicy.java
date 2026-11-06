package com.adminpro.security;

import java.nio.charset.StandardCharsets;

import com.adminpro.exception.ApiException;

/**
 * BCrypt admite 72 bytes, no 72 caracteres: una tilde ocupa dos bytes en UTF-8, así que
 * una contraseña válida para @Size podría exceder el límite y fallar con un error 500.
 */
public final class PasswordPolicy {

    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean cabeEnBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }

    public static void validar(String campo, String password) {
        if (!cabeEnBcrypt(password)) {
            throw ApiException.campo(campo, "La contraseña es demasiado larga");
        }
    }
}
