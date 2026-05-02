package br.com.coopticket.usuario.service;

import br.com.coopticket.usuario.dto.RegisterRequestDto;
import br.com.coopticket.usuario.dto.RegisterResponseDto;

public interface IUsuarioService {
    RegisterResponseDto registrar(RegisterRequestDto body);
}
