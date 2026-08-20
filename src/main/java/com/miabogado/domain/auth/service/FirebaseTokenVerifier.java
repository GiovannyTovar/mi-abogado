package com.miabogado.domain.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.miabogado.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Unico punto del sistema que habla con Firebase. Verifica la firma, el emisor,
 * la audiencia y la expiracion del ID token contra las claves publicas de Google.
 */
@Service
@RequiredArgsConstructor
public class FirebaseTokenVerifier {

    /** ObjectProvider: en tests/local sin credenciales el bean no existe y la app igual arranca. */
    private final ObjectProvider<FirebaseAuth> firebaseAuth;

    public GoogleIdentity verify(String idToken) {
        FirebaseAuth auth = firebaseAuth.getIfAvailable();
        if (auth == null) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Autenticacion con Google no configurada en este entorno");
        }

        try {
            FirebaseToken token = auth.verifyIdToken(idToken, true);
            return new GoogleIdentity(
                    token.getUid(),
                    token.getEmail(),
                    token.isEmailVerified(),
                    token.getName(),
                    token.getPicture());
        } catch (FirebaseAuthException e) {
            throw BusinessException.unauthorized("Token de Google invalido o expirado");
        }
    }
}
