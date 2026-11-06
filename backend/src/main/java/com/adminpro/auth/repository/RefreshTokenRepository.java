package com.adminpro.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.adminpro.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** El bloqueo evita que dos peticiones simultáneas roten el mismo token. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> buscarParaRotar(@Param("hash") String hash);

    Optional<RefreshToken> findByTokenHash(String hash);

    boolean existsByFamiliaIdAndRevocadoAtIsNullAndExpiraAtAfter(UUID familiaId, Instant ahora);

    @Modifying
    @Query("""
            update RefreshToken t set t.revocadoAt = :ahora, t.updatedAt = :ahora
            where t.familiaId = :familia and t.revocadoAt is null
            """)
    int revocarFamilia(@Param("familia") UUID familia, @Param("ahora") Instant ahora);

    @Modifying
    @Query("""
            update RefreshToken t set t.revocadoAt = :ahora, t.updatedAt = :ahora
            where t.usuarioId = :usuario and t.revocadoAt is null
            """)
    int revocarDeUsuario(@Param("usuario") Long usuario, @Param("ahora") Instant ahora);

    @Modifying
    @Query("""
            update RefreshToken t set t.revocadoAt = :ahora, t.updatedAt = :ahora
            where t.usuarioId = :usuario and t.familiaId <> :familia and t.revocadoAt is null
            """)
    int revocarOtrasFamilias(@Param("usuario") Long usuario, @Param("familia") UUID familia,
            @Param("ahora") Instant ahora);
}
