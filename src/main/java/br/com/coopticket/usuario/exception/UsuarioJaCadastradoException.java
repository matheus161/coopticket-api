package br.com.coopticket.usuario.exception;

import br.com.coopticket.infra.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UsuarioJaCadastradoException extends BusinessException {
    public UsuarioJaCadastradoException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
