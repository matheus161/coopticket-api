package br.com.coopticket.infra.dto;

import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDto(int status, String mensagem, List<FieldError> errors, LocalDateTime timestamp){
    public record FieldError(String field, String message) {}
}
