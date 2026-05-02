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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    public RegisterResponseDto registrar(RegisterRequestDto body) {
        Optional<Usuario> existingUser = usuarioRepository.findByEmail(body.email());

        if (existingUser.isPresent()) {
            throw new UsuarioJaCadastradoException(UsuarioConstants.EMAIL_JA_CADASTRADO);
        }

        String hashedPassword = passwordEncoder.encode(body.senha());
        Usuario createdUser = new Usuario(body.nome(), body.email(), hashedPassword);
        usuarioRepository.save(createdUser);

        String token = tokenService.generateToken(createdUser.getEmail());

        return new RegisterResponseDto(createdUser.getEmail(), token);
    }
}
