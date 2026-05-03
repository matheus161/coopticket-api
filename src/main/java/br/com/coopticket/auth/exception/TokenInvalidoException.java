package br.com.coopticket.auth.exception;

import br.com.coopticket.infra.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class TokenInvalidoException extends BusinessException {
    public TokenInvalidoException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
