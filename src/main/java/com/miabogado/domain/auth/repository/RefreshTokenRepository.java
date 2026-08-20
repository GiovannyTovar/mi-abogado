package com.miabogado.domain.auth.repository;

import com.miabogado.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** El refresh emite un JWT nuevo, que necesita el usuario y su tenant: se traen ya resueltos. */
    @EntityGraph(attributePaths = {"user", "user.tenant"})
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Cierre de sesion en todos los dispositivos y revocacion al desactivar un usuario. */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAllByUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
