package br.com.coopticket.usuario.service;

import br.com.coopticket.usuario.BaseIntegrationTest;
import br.com.coopticket.usuario.UsuarioConstants;
import br.com.coopticket.usuario.dto.RegisterRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioServiceIntegrationTest extends BaseIntegrationTest {

    private static final String ENDPOINT = "/usuario/registrar";
    private static final String NOME = "João Silva";
    private static final String EMAIL = "joao@email.com";
    private static final String SENHA = "Senha123";

    @Test
    void deveCriarUsuarioComSucesso() throws Exception {
        var request = new RegisterRequestDto(NOME, EMAIL, SENHA);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.token").isNotEmpty());

        var usuarioSalvo = buscarUsuarioPorEmail(EMAIL);
        assertThat(usuarioSalvo.getNome()).isEqualTo(NOME);
        assertThat(usuarioSalvo.isAtivo()).isTrue();
        assertThat(usuarioSalvo.getSenhaHash()).isNotEqualTo(SENHA);
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaCadastrado() throws Exception {
        var request = new RegisterRequestDto(NOME, EMAIL, SENHA);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(UsuarioConstants.EMAIL_JA_CADASTRADO));
    }

    @Test
    void deveRetornar400QuandoNomeAusente() throws Exception {
        var request = new RegisterRequestDto(null, EMAIL, SENHA);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoEmailAusente() throws Exception {
        var request = new RegisterRequestDto(NOME, null, SENHA);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoEmailInvalido() throws Exception {
        var request = new RegisterRequestDto(NOME, "email-invalido", SENHA);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoSenhaAusente() throws Exception {
        var request = new RegisterRequestDto(NOME, EMAIL, null);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoSenhaInvalida() throws Exception {
        var request = new RegisterRequestDto(NOME, EMAIL, "fraca");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarTokenValidoAposCadastro() throws Exception {
        String token = registrarEObterToken(NOME, EMAIL, SENHA);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void deveSalvarSenhaHasheadaEDiferenteDaOriginal() throws Exception {
        var request = new RegisterRequestDto(NOME, EMAIL, SENHA);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var usuario = buscarUsuarioPorEmail(EMAIL);
        assertThat(usuario.getSenhaHash()).isNotEqualTo(SENHA);
        assertThat(usuario.getSenhaHash()).startsWith("$2a$");
    }

    @Test
    void deveCriarUsuariosComEmailsDiferentesIndependentemente() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequestDto(NOME, EMAIL, SENHA))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequestDto("Maria Silva", "maria@email.com", SENHA))))
                .andExpect(status().isCreated());

        assertThat(usuarioRepository.count()).isEqualTo(2);
    }
}
