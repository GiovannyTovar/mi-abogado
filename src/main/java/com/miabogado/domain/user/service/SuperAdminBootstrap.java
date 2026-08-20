package com.miabogado.domain.user.service;

import com.miabogado.domain.user.entity.Role;
import com.miabogado.domain.user.entity.User;
import com.miabogado.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el problema del huevo y la gallina: sin auto-registro, alguien tiene que
 * existir antes del primer login. Deja creado el super-admin en estado PENDING; queda
 * ACTIVE cuando esa persona entra con Google. Idempotente.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.bootstrap", name = "super-admin-email")
public class SuperAdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final String superAdminEmail;

    public SuperAdminBootstrap(UserRepository userRepository,
                               @Value("${app.bootstrap.super-admin-email}") String superAdminEmail) {
        this.userRepository = userRepository;
        this.superAdminEmail = superAdminEmail;
    }

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (superAdminEmail.isBlank()) {
            return;
        }
        if (userRepository.findByEmailAndRole(superAdminEmail, Role.SUPER_ADMIN).isPresent()) {
            return;
        }
        userRepository.save(new User(null, superAdminEmail, "Super Admin", Role.SUPER_ADMIN));
        log.info("Super-admin inicial creado para {} (pendiente de primer login con Google)", superAdminEmail);
    }
}
