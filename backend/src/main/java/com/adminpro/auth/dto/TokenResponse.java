package com.adminpro.auth.dto;

public record TokenResponse(String accessToken, long expiresIn, UsuarioActualResponse usuario) {
}
