# Criar nova migration Flyway

Você vai criar uma nova migration Flyway seguindo o padrão do projeto.

## Argumentos esperados
- Descrição curta da migration em português, em snake_case (ex: `adicionar_coluna_observacao_passagem`)
- Conteúdo SQL ou descrição do que a migration deve fazer

Se não foi fornecido, pergunte.

## Passos

### 1. Determinar próximo número de versão
- Liste arquivos em `src/main/resources/db/migration/`
- Identifique o maior número V<n>
- A nova migration será V<n+1>

### 2. Criar arquivo
Nome: `V<n+1>__<descricao>.sql`

Template:
```sql
-- Migration: V<n+1>
-- Descrição: <descrição em português>
-- Data: <data atual>
-- Autor: <pegar via git config user.name se disponível>

-- ============================================================
-- INÍCIO DA MIGRATION
-- ============================================================

<conteúdo SQL>

-- ============================================================
-- FIM DA MIGRATION
-- ============================================================
```

### 3. Validar SQL
- Verifique que usa sintaxe PostgreSQL
- Verifique que mudanças em tabelas existentes usam ALTER TABLE
- Sugira índices se a migration adiciona colunas usadas em WHERE/JOIN frequentes
- Para CREATE TABLE, lembre de incluir as colunas padrão `criado_em`, `atualizado_em` se for tabela de domínio

### 4. Reportar
- Mostre o caminho completo do arquivo criado
- Lembre o usuário de:
  - Testar a migration localmente: `./gradlew flywayMigrate` ou apenas reiniciar a aplicação
  - **NUNCA** editar uma migration depois de aplicada em produção — sempre criar nova
  - Conferir que migrations são idempotentes ou usam `IF NOT EXISTS` quando apropriado
