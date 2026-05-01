# CoopTicket — Contexto do Projeto

## O que é

API Spring Boot para gestão operacional e financeira de transporte intermunicipal. O **veículo é a entidade central** do modelo — não a cooperativa. Cada veículo opera em um de três modos: linha regular (horários fixos), sob demanda (sai quando enche) ou táxi.

Público-alvo inicial: cooperativas de ônibus intermunicipais. Expande para taxistas e autônomos nas fases seguintes.

## Stack

- Java 21, Spring Boot 3.5.14, Maven
- PostgreSQL 15+ com Flyway para migrations
- Spring Security + JWT para autenticação
- Spring Data JPA com lock otimista (`@Version`) para controle de vagas
- EFÍ (ou Pagar.me) para Pix
- Hospedagem prevista: Railway (back-end + Postgres)

## Arquitetura

**Monolito modular com package-by-feature**. Cada módulo em `br.com.coopticket.<modulo>` com subpastas `controller`, `service`, `repository`, `domain`, `dto`.

Regra de ouro: módulos se comunicam **apenas via service de outro módulo**, nunca acessando repository de outro módulo diretamente. Isso preserva limites e prepara para Spring Modulith ou microsserviços no futuro.

Documentação detalhada da arquitetura em `docs/arquitetura-decisoes.md` e diagrama em `docs/diagramas/arquitetura-containers.mermaid`.

## Modelo de dados

DER completo em `docs/diagramas/der.dbml` (formato dbdiagram.io) e `docs/diagramas/der.mermaid`.

Decisões de modelagem importantes em `docs/der-decisoes.md`. Pontos críticos:

- `veiculo_id` aparece em quase toda tabela operacional — é o eixo central
- `horario_template` (grade recorrente) separado de `viagem_instancia` (viagem real do dia)
- `viagem_instancia.template_id` é nullable — modos `SOB_DEMANDA` e `TAXI` criam instâncias direto
- `passagem.id_externo` com unique parcial — idempotência do webhook da cooperativa
- `viagem_instancia.versao` para lock otimista do JPA na atualização de vagas
- `snapshot_viagem` é tabela imutável (auditoria de fechamento de viagem)

## Fluxos críticos

Documentação completa em `docs/sequencia-decisoes.md` e diagramas em `docs/diagramas/sequencia-venda-passagem.mermaid`.

Três fluxos principais:
1. Venda via app com Pix (caminho feliz)
2. Polling de fallback quando webhook do Pix não chega
3. Webhook da cooperativa para venda em guichê (com idempotência por `id_externo`)

Vagas só decrementam quando passagem confirma (status CONFIRMADA), nunca na criação. Overbooking é permitido com sinalização — guichê tem prioridade na reconciliação.

## Plano de fases

Plano completo em `docs/documentacao-sistema.md`. Resumo:

- Fase 0: Fundação (banco, JWT, perfis)
- Fase 1A: Cadastro de veículos
- Fase 1B: Horários e geração de viagens
- Fase 1C: Operação de passagens com Pix
- Fase 1D: Comprovante via WhatsApp
- Fase 1E: Despesas e dashboard financeiro
- Fase 1F: Webhook da cooperativa
- Fase 2+: Plataforma aberta, NFC, app passageiro, GPS

## Convenções de código

- Nomes em português para domínio (entidades, services, controllers): `Veiculo`, `VeiculoService`, `VeiculoController`
- Termos técnicos do framework em inglês (Repository, Controller, Service)
- DTOs sempre como `record` (Java 21)
- Validação via Bean Validation (`@NotNull`, `@Valid`) nos DTOs de entrada
- Exceções de negócio estendem `BusinessException` em `infra.exception`
- Nunca usar `Optional` como parâmetro de método (apenas como retorno)
- Nunca expor entidades JPA em controllers — sempre mapear para DTO
- Migrations Flyway em `src/main/resources/db/migration/` com versionamento `V1__`, `V2__`, etc.

## Convenções de teste

- Testes unitários em `src/test/java/br/com/coopticket/<modulo>/` espelhando a estrutura de produção
- Integração via Testcontainers (Postgres real em container)
- Nomes de teste em português: `deveCriarPassagemQuandoVagaDisponivel()`

## Perfis de usuário e permissões

| Perfil | Escopo |
|---|---|
| ADMIN_PLATAFORMA | Tudo |
| ADMIN_COOPERATIVA | Templates de horário e rotas da cooperativa |
| PROPRIETARIO | Seus veículos: financeiro, rotas, cobradores, webhook |
| GESTOR_OPERACIONAL | Configuração de rotas/escalas (sem financeiro) |
| COBRADOR | Operação de um veículo: vender, lançar despesa |
| MOTORISTA_AUTONOMO | Proprietário + cobrador da própria operação |

## O que NÃO está no MVP

Não implementar até que seja explicitamente pedido:
- Cancelamento de passagem com estorno (Fase 2)
- Compra de passagem pelo passageiro (Fase 2)
- NFC e maquininha (Fase 2)
- Rastreamento GPS (Fase 3)
- Impressão térmica (Fase 2.5)
- Spring Modulith (avaliar quando código crescer)
- Cache Redis (não necessário no MVP)
- Microsserviços (monolito é a escolha consciente)

## Observações importantes para o Claude Code

- Sempre que criar uma entidade JPA nova, criar a migration Flyway correspondente
- Sempre que adicionar um endpoint, garantir que está protegido pelo perfil correto via Spring Security
- Ao implementar lógica de venda/vagas, sempre considerar concorrência (`@Version`)
- Ao integrar serviços externos (EFÍ), sempre considerar idempotência e fallback
- Antes de criar lógica complexa, consultar os documentos em `docs/` para entender o contexto
