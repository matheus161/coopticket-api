package br.com.coopticket.usuario.service;

import br.com.coopticket.auth.service.TokenService;
import br.com.coopticket.usuario.UsuarioConstants;
import br.com.coopticket.usuario.domain.Usuario;
import br.com.coopticket.usuario.dto.RegisterRequestDto;
import br.com.coopticket.usuario.dto.RegisterResponseDto;
import br.com.coopticket.usuario.exception.UsuarioJaCadastradoException;
import br.com.coopticket.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceUnitTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveCriarUsuarioQuandoEmailNaoCadastrado() {
        var request = new RegisterRequestDto("João Silva", "joao@email.com", "Senha123");

        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("hash_senha");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenService.generateToken("joao@email.com")).thenReturn("token_jwt");

        RegisterResponseDto response = usuarioService.registrar(request);

        assertThat(response.email()).isEqualTo("joao@email.com");
        assertThat(response.token()).isEqualTo("token_jwt");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaCadastrado() {
        var request = new RegisterRequestDto("João Silva", "joao@email.com", "Senha123");
        var usuarioExistente = new Usuario("João Silva", "joao@email.com", "hash_antiga");

        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuarioExistente));

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(UsuarioJaCadastradoException.class)
                .hasMessage(UsuarioConstants.EMAIL_JA_CADASTRADO);

        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(tokenService, never()).generateToken(anyString());
    }
}
