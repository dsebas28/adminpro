package com.adminpro.auth.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.adminpro.common.BaseEntity;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "familia_id", nullable = false)
    private UUID familiaId;

    @Column(name = "expira_at", nullable = false)
    private Instant expiraAt;

    @Column(name = "revocado_at")
    private Instant revocadoAt;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    protected RefreshToken() {
    }

    public RefreshToken(Long usuarioId, String tokenHash, UUID familiaId, Instant expiraAt, String ip,
            String userAgent) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.familiaId = familiaId;
        this.expiraAt = expiraAt;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public void revocar(Instant ahora) {
        revocadoAt = ahora;
    }

    public boolean estaVencido(Instant ahora) {
        return !expiraAt.isAfter(ahora);
    }

    public boolean estaRevocado() {
        return revocadoAt != null;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public UUID getFamiliaId() {
        return familiaId;
    }

    public Instant getRevocadoAt() {
        return revocadoAt;
    }
}
