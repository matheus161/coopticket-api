package br.com.coopticket.auth.service;

import br.com.coopticket.auth.exception.TokenInvalidoException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    public String generateToken(String email) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("coopticket-api")
                    .withSubject(email)
                    .withExpiresAt(Instant.now().plusMillis(expirationMs))
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new TokenInvalidoException("Erro ao gerar token de autenticação");
        }
    }

    public Optional<String> validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String subject = JWT.require(algorithm)
                    .withIssuer("coopticket-api")
                    .build()
                    .verify(token)
                    .getSubject();
            return Optional.of(subject);
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
