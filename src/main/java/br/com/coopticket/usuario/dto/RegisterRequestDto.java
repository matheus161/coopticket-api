package br.com.coopticket.usuario.dto;

import br.com.coopticket.usuario.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequestDto(
        @NotNull(message = "O nome é obrigatório")
        String nome,

        @Email(message = "E-mail inválido")
        @NotBlank(message = "O e-mail é obrigatório")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @ValidPassword
        String senha
) {}
