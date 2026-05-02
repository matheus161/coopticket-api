package br.com.coopticket.usuario.controller;

import br.com.coopticket.usuario.dto.RegisterRequestDto;
import br.com.coopticket.usuario.dto.RegisterResponseDto;
import br.com.coopticket.usuario.service.IUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("usuario")
@RequiredArgsConstructor
public class UsuarioController {
    private final IUsuarioService usuarioService;

    @PostMapping("/registrar")
    public ResponseEntity<RegisterResponseDto> registrar(@RequestBody @Valid RegisterRequestDto body) {
        return ResponseEntity.ok(usuarioService.registrar(body));
    }
}
