-- Migration: V1
-- Descrição: Criar tabela de usuários com autenticação
-- Data: 2026-05-02
-- Autor: Matheus Lima

-- ============================================================
-- INÍCIO DA MIGRATION
-- ============================================================

CREATE TABLE usuario (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(150)             NOT NULL,
    email           VARCHAR(255)             NOT NULL,
    senha_hash      VARCHAR(255)             NOT NULL,
    ativo           BOOLEAN                  NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_usuario_email ON usuario(email);

-- ============================================================
-- FIM DA MIGRATION
-- ============================================================
