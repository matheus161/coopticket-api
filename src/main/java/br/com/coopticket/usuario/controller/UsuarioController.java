package br.com.coopticket.usuario.controller;

import br.com.coopticket.infra.dto.ErrorResponseDto;
import br.com.coopticket.usuario.dto.RegisterRequestDto;
import br.com.coopticket.usuario.dto.RegisterResponseDto;
import br.com.coopticket.usuario.service.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuario")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Registro e autenticação de usuários")
public class UsuarioController {

    private final IUsuarioService usuarioService;

    @PostMapping("/registrar")
    @Operation(summary = "Registrar novo usuário", description = "Cria um novo usuário e retorna um token JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
            content = @Content(schema = @Schema(implementation = RegisterResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<RegisterResponseDto> registrar(@RequestBody @Valid RegisterRequestDto body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(body));
    }
}
