package br.com.coopticket.usuario.service;

import br.com.coopticket.auth.service.TokenService;
import br.com.coopticket.usuario.UsuarioConstants;
import br.com.coopticket.usuario.domain.Usuario;
import br.com.coopticket.usuario.dto.RegisterRequestDto;
import br.com.coopticket.usuario.dto.RegisterResponseDto;
import br.com.coopticket.usuario.exception.UsuarioJaCadastradoException;
import br.com.coopticket.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService implements IUsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional
    public RegisterResponseDto registrar(RegisterRequestDto body) {
        if (usuarioRepository.existsByEmail(body.email())) {
            throw new UsuarioJaCadastradoException(UsuarioConstants.EMAIL_JA_CADASTRADO);
        }

        String senhaHash = passwordEncoder.encode(body.senha());
        Usuario novoUsuario = new Usuario(body.nome(), body.email(), senhaHash);
        usuarioRepository.save(novoUsuario);

        String token = tokenService.generateToken(novoUsuario.getEmail());
        return new RegisterResponseDto(novoUsuario.getEmail(), token);
    }
}
