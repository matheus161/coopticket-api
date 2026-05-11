# DER — Documentação de Modelagem

Este documento acompanha os arquivos `der.dbml` (visualização interativa em dbdiagram.io) e `der.mermaid` (renderização inline no README).

## Visão geral

O modelo tem **17 tabelas** organizadas em 6 grupos:

1. **Identidade**: `usuario`, `perfil`, `usuario_perfil`
2. **Organização**: `cooperativa`, `veiculo`, `vinculo_cobrador`, `rota`
3. **Horários e viagens**: `horario_template`, `excecao_template`, `viagem_instancia`
4. **Operação**: `passagem`, `pagamento_pix`, `comprovante`
5. **Integração e financeiro**: `webhook_config`, `webhook_log`, `despesa`, `snapshot_viagem`
6. **Auditoria**: `evento_auditoria`

## Decisões de modelagem importantes

### Soft delete em despesa com auditoria de quem excluiu

A tabela `despesa` tem campos `excluida_em` (timestamp) e `excluida_por_id` (FK para usuario). Por baixo, exclusão é soft — preserva histórico completo. Por cima, UX mostra como remoção limpa (despesa some da lista).

A regra de negócio "cobrador só exclui no mesmo dia" é aplicada na camada de serviço:

```java
if (despesa.getRegistradoPor() != usuarioAtual && !ehProprietario)
    throw new SemPermissaoException("Você não pode excluir despesa de outro usuário");

if (despesa.getDataDespesa() != hoje && !ehProprietario)
    throw new SemPermissaoException("Despesa só pode ser excluída no mesmo dia");
```

Proprietário do veículo não tem essas restrições — pode excluir qualquer despesa em qualquer data.

### Veículo é o eixo central, não a cooperativa

`veiculo_id` aparece em quase toda tabela operacional (`passagem`, `despesa`, `viagem_instancia`, `webhook_config`, `vinculo_cobrador`). Cooperativa é opcional (`veiculo.cooperativa_id` é nullable). Isso garante que veículos autônomos funcionem sem nenhuma cooperativa cadastrada.

### Separação template × instância

`horario_template` é a grade recorrente (toda segunda às 06h). `viagem_instancia` é a viagem real do dia (03/05/2026 06h). Vagas, passagens, status e fechamento moram na **instância**. Editar template não afeta instâncias já geradas.

`viagem_instancia.template_id` é **nullable** porque veículos `SOB_DEMANDA` e `TAXI` criam instâncias direto, sem template.

### Rotas e templates: XOR entre cooperativa e veículo

Tanto `rota` quanto `horario_template` têm `cooperativa_id` E `veiculo_id`, mas exatamente um deve estar preenchido. Isso é garantido por uma CHECK constraint:

```sql
ALTER TABLE rota ADD CONSTRAINT rota_xor_owner
  CHECK ((cooperativa_id IS NOT NULL)::int + (veiculo_id IS NOT NULL)::int = 1);

ALTER TABLE horario_template ADD CONSTRAINT template_xor_owner
  CHECK ((cooperativa_id IS NOT NULL)::int + (veiculo_id IS NOT NULL)::int = 1);
```

A alternativa seria duas tabelas separadas (`rota_cooperativa` e `rota_veiculo`), mas isso explode o número de joins e foreign keys nas tabelas que consomem rota.

### Vagas e overbooking — reserva otimista

`viagem_instancia.vagas_vendidas` pode ultrapassar `vagas_total`. Quando isso acontece, a passagem é marcada com `overbooking = true`. Não há lock pessimista; usamos a coluna `versao` para lock otimista do JPA (`@Version`) para evitar race conditions na atualização do contador.

A regra de prioridade do guichê é aplicada **na reconciliação**, não no momento da venda: passagens vindas do webhook (`canal = GUICHE`) sempre confirmam vaga, mesmo que cause overbooking nas passagens do app.

### `id_externo` na passagem como chave de idempotência

Quando o webhook da cooperativa envia `id_externo = "VND-12345"`, o sistema procura passagem com esse mesmo `id_externo`. Se existe, retorna 409 (já processada). Se não existe, cria. Isso resolve duplicação por retry sem precisar de tabela auxiliar.

A constraint é unique parcial (apenas quando `id_externo IS NOT NULL`):

```sql
CREATE UNIQUE INDEX uq_passagem_id_externo
  ON passagem (id_externo)
  WHERE id_externo IS NOT NULL;
```

### `veiculo_id` desnormalizado em `passagem`

Tecnicamente seria possível chegar no veículo via `passagem → viagem_instancia → veiculo`. Desnormalizei porque queries de relatório financeiro por veículo são frequentes e o join extra atrapalha índices.

Cuidado: ao salvar passagem, validar que `passagem.veiculo_id == viagem_instancia.veiculo_id`. Isso pode ser feito no service ou via trigger.

### `snapshot_viagem` como tabela imutável

Quando uma viagem é encerrada, gera-se um snapshot agregado (total vendido, receita, número de overbookings). Esse registro **nunca é atualizado**. Se houver erro, cria-se um novo snapshot de correção referenciando o original.

Por que não calcular sob demanda via SUM? Porque preço da rota e nome da rota podem mudar depois. O snapshot congela o estado da época para auditoria fiel.

### `evento_auditoria` é granular, snapshot é agregado

São complementares. `evento_auditoria` registra cada mudança individual ("passagem X foi cancelada às 14h32"). `snapshot_viagem` agrega o resultado final ("viagem Y vendeu R$ 480,00 em 32 passagens"). Auditoria fina + visão executiva.

### Sem soft delete generalizado

Em vez de `deleted_at` em toda tabela, usamos `ativo boolean`. Razão: `deleted_at` tende a virar bagunça (alguns sistemas filtram, outros não). `ativo = false` é explícito.

Exceções: `passagem` e `viagem_instancia` não têm `ativo`. Use `status = CANCELADA` em vez disso, porque o estado conta para o histórico.

## Constraints e CHECK críticas

```sql
-- XOR de owner em rota e template (já mencionado)

-- modo_operacao válido
ALTER TABLE veiculo ADD CONSTRAINT veiculo_modo_valido
  CHECK (modo_operacao IN ('LINHA_REGULAR', 'SOB_DEMANDA', 'TAXI'));

-- status de viagem
ALTER TABLE viagem_instancia ADD CONSTRAINT viagem_status_valido
  CHECK (status IN ('PROGRAMADA', 'EM_ANDAMENTO', 'ENCERRADA', 'CANCELADA'));

-- canal de venda
ALTER TABLE passagem ADD CONSTRAINT passagem_canal_valido
  CHECK (canal IN ('APP', 'GUICHE', 'MANUAL'));

-- coerência de veículo entre passagem e viagem
-- (validado no service; trigger é alternativa)

-- vagas não negativas
ALTER TABLE viagem_instancia ADD CONSTRAINT vagas_total_positiva
  CHECK (vagas_total > 0);

-- bitmask de dias da semana válida (1 a 127)
ALTER TABLE horario_template ADD CONSTRAINT dias_semana_valido
  CHECK (dias_semana BETWEEN 1 AND 127);
```

## Índices recomendados além das chaves

Os índices mais importantes para performance no MVP:

```sql
-- Listar viagens de hoje de um veículo (consulta principal do app do cobrador)
CREATE INDEX idx_viagem_veiculo_data
  ON viagem_instancia (veiculo_id, partida_prevista)
  WHERE status IN ('PROGRAMADA', 'EM_ANDAMENTO');

-- Relatório financeiro por veículo e período
CREATE INDEX idx_passagem_veiculo_periodo
  ON passagem (veiculo_id, criado_em)
  WHERE status IN ('PAGA', 'CONFIRMADA');

-- Despesas por veículo e período
CREATE INDEX idx_despesa_veiculo_data
  ON despesa (veiculo_id, data_despesa);

-- Job que gera instâncias: busca templates ativos
CREATE INDEX idx_template_ativo_vigencia
  ON horario_template (ativo, vigencia_inicio, vigencia_fim)
  WHERE ativo = true;

-- Busca de pagamento Pix por txid (callback do gateway)
-- já coberto pelo unique em txid

-- Auditoria: buscar histórico de uma entidade específica
CREATE INDEX idx_auditoria_entidade
  ON evento_auditoria (entidade, entidade_id, ocorrido_em);
```

## Valores iniciais (seed) necessários

A tabela `perfil` é catálogo fixo. Seed inicial:

```sql
INSERT INTO perfil (id, nome) VALUES
  (1, 'ADMIN_PLATAFORMA'),
  (2, 'ADMIN_COOPERATIVA'),
  (3, 'PROPRIETARIO'),
  (4, 'GESTOR_OPERACIONAL'),
  (5, 'COBRADOR'),
  (6, 'MOTORISTA_AUTONOMO');
```

## Migrations

Recomendação: usar **Flyway** com Spring Boot. Estrutura sugerida:

```
src/main/resources/db/migration/
  V1__schema_inicial.sql           -- todas as tabelas e índices
  V2__seed_perfis.sql              -- inserts dos perfis
  V3__check_constraints.sql        -- CHECK constraints
  V4__indices_performance.sql      -- índices não-óbvios
```

Cada fase do plano de desenvolvimento gera novas migrations versionadas. **Nunca editar uma migration já aplicada em produção** — sempre criar nova.

## Pontos de atenção para a implementação

**Atomicidade na venda de passagem**: criar passagem + criar pagamento_pix + incrementar `vagas_vendidas` precisa ser uma transação só. Se o gateway de pagamento falhar, rollback de tudo.

**Consistência do contador de vagas**: ao confirmar passagem (status muda para CONFIRMADA), incrementa o contador. Ao cancelar (futuro), decrementa. Use `@Version` no JPA para detectar concorrência.

**Job de geração de instâncias**: idempotente (verifica se instância já existe antes de criar). Roda 1x por dia, gera próximos 7 dias. Respeita `excecao_template`.

**Job de fechamento automático**: instâncias com `partida_prevista < now() - 24h` e status `PROGRAMADA` ou `EM_ANDAMENTO` são fechadas automaticamente, gerando snapshot.

**Limpeza de Pix expirados**: pagamentos com `status = PENDENTE` e `expira_em < now()` viram `EXPIRADO`. Passagem associada vira `EXPIRADA`. Vagas não foram decrementadas ainda (só decrementam quando confirma), então não precisa rollback.
