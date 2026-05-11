# CoopTicket — Contexto do Projeto

## O que é

Plataforma de gestão operacional e financeira para transporte intermunicipal. O **veículo é a entidade central** do modelo — não a cooperativa. Cada veículo opera em um de três modos: linha regular (horários fixos), sob demanda (sai quando enche) ou táxi.

**Filosofia de produto**: autonomia do operador. Qualquer cooperado pode configurar seu próprio ambiente sem depender de cooperativa. A plataforma serve o operador (proprietário, motorista autônomo, cobrador), não a cooperativa.

Público-alvo inicial: motoristas autônomos e proprietários de veículos individuais. Cooperativas como cliente entram em fase posterior, com painel web próprio.

## Estratégia de plataforma

**MVP é mobile-first.** O app React Native cobre o ciclo completo: operação, financeiro, gestão de veículos, configuração de horários. Painel web fica para fase posterior (Fase 2+), focado em administração de cooperativas e relatórios consolidados pesados.

Justificativa: produto único e completo entregue vale mais que duas frentes pela metade. Operadores em campo vivem no celular — web seria pouco usada por eles mesmo se existisse. Web vira valiosa só quando entrarem cooperativas como cliente.

## Stack

- Java 21, Spring Boot 3.5.14, Gradle Groovy
- PostgreSQL 15+ com Flyway para migrations
- Spring Security + JWT para autenticação
- Spring Data JPA com lock otimista (`@Version`) para controle de vagas
- EFÍ (ou Pagar.me) para Pix
- React Native + Expo para o app mobile
- Hospedagem prevista: Railway (back-end + Postgres)

## Arquitetura

**Monolito modular com package-by-feature**. Cada módulo em `br.com.coopticket.<modulo>` com subpastas `controller`, `service`, `repository`, `domain`, `dto`.

Regra de ouro: módulos se comunicam **apenas via service de outro módulo**, nunca acessando repository de outro módulo diretamente.

Documentação detalhada em `docs/arquitetura-decisoes.md` e diagrama em `docs/diagramas/arquitetura-containers.mermaid`.

## Modelo de dados

DER completo em `docs/der.dbml` e `docs/diagramas/der.mermaid`. Decisões em `docs/der-decisoes.md`. Pontos críticos:

- `veiculo_id` aparece em quase toda tabela operacional — é o eixo central
- `horario_template` (grade recorrente) separado de `viagem_instancia` (viagem real do dia)
- `viagem_instancia.template_id` é nullable — modos `SOB_DEMANDA` e `TAXI` criam instâncias direto
- `passagem.id_externo` com unique parcial — idempotência do webhook da cooperativa
- `viagem_instancia.versao` para lock otimista do JPA na atualização de vagas
- `snapshot_viagem` é tabela imutável (auditoria de fechamento de viagem)
- `passagem.valor_sugerido` registra o valor padrão da viagem; `valor` é o efetivamente cobrado (permite descontos para trechos intermediários)
- `passagem.motivo_desconto` opcional, coletado quando valor cobrado < valor sugerido

## Decisões de escopo do MVP

**Cooperativa**: ignorada no MVP. Campo `cooperativa_id` no `veiculo` continua existindo no schema mas fica sempre `null`. Templates de horário ficam vinculados diretamente ao veículo. Quando lançar painel web para cooperativas (fase posterior), o campo será preenchido e a herança de templates passa a funcionar.

**Cancelamento de passagem com estorno**: fase 2.

**Cancelamento de conta de usuário**: fora do MVP. Solicitações são tratadas manualmente pelo admin da plataforma direto no banco.

**Login com Google (OAuth)**: fora do MVP. Apenas email/senha.

**Comprovante**: PNG via WhatsApp share intent (sem API paga). Impressão térmica fica para fase 2.5.

**Email transacional**: fora do MVP. Apenas WhatsApp e download de imagem.

**App do passageiro**: fase posterior, projeto separado (não dentro do app do operador).

## Fluxos críticos

Documentação completa em `docs/sequencia-decisoes.md` e diagramas em `docs/diagramas/sequencia-venda-passagem.mermaid`.

Três fluxos principais:
1. Venda via app com Pix (caminho feliz)
2. Polling de fallback quando webhook do Pix não chega
3. Webhook da cooperativa para venda em guichê (com idempotência por `id_externo`) — implementação adiada para fase com cooperativa

Vagas só decrementam quando passagem confirma (status CONFIRMADA), nunca na criação. Overbooking é permitido com sinalização e confirmação dupla na interface.

## Modelo de operação do app mobile

**Onboarding inicial decide o caminho do usuário:**

Após login/cadastro, usuário escolhe entre:
- **Inserir código de convite** → vincula como cobrador de veículo existente → vai para Operação
- **Cadastrar próprio veículo** → vira proprietário → cadastro obrigatório de finalizar antes de navegar

Usuário sem nenhum vínculo ativo (cobrador removido de todos os veículos, proprietário sem veículos) cai na tela de propósito novamente.

**Estrutura de navegação por tab bar inferior:**

- **Operação**: viagens, venda de passagens (sempre presente)
- **Financeiro**: receita, despesas, snapshots (sempre presente)
- **Veículos**: gestão de veículos próprios (só aparece se for proprietário)
- **Conta**: perfil, configurações (sempre presente)

**Remoção de cobrador**: soft delete no banco (preserva histórico em `vinculo_cobrador.desvinculado_em`), interface mostra como remoção limpa.

## Plano de fases

Plano completo em `docs/documentacao-sistema.md`. Resumo atualizado:

- Fase 0: Fundação (banco, JWT, perfis) — em andamento
- Fase 1A: Cadastro de veículos (mobile)
- Fase 1B: Horários e geração de viagens (mobile)
- Fase 1C: Operação de passagens com Pix (mobile)
- Fase 1D: Comprovante via WhatsApp (mobile)
- Fase 1E: Despesas e dashboard financeiro (mobile)
- Fase 2: Painel web para cooperativas + app do passageiro + cancelamento + Google login
- Fase 2.5: Impressão térmica + fretes pré-agendados
- Fase 3: Rastreamento GPS em tempo real

## Convenções de código

- Nomes em português para domínio (entidades, services, controllers): `Veiculo`, `VeiculoService`, `VeiculoController`
- Termos técnicos do framework em inglês (Repository, Controller, Service)
- DTOs sempre como `record` (Java 21)
- Validação via Bean Validation (`@NotNull`, `@Valid`) nos DTOs de entrada
- Exceções de negócio estendem `BusinessException` em `infra.exception`
- Nunca usar `Optional` como parâmetro de método (apenas como retorno)
- Nunca expor entidades JPA em controllers — sempre mapear para DTO
- Migrations Flyway em `src/main/resources/db/migration/` com versionamento `V1__`, `V2__`, etc.
- `@Transactional(readOnly = true)` por padrão na classe Service; métodos de escrita marcados explicitamente com `@Transactional`
- Injeção via construtor (sem `@Autowired` em campo)
- Endpoints versionados em `/api/v1/...` no plural (`/usuarios`, não `/usuario`)
- Verbos não vão na URL (POST `/usuarios`, não POST `/usuarios/registrar`)
- SpringDoc OpenAPI: confiar na inferência automática (schema, status 201/400). Documentar manualmente apenas o que é regra de negócio (status 409, 422, exemplos). Erros centralizados no `GlobalExceptionHandler`.

## Convenções de teste

- Testes unitários em `src/test/java/br/com/coopticket/<modulo>/` espelhando a estrutura de produção
- Integração via Testcontainers (Postgres real em container)
- Nomes de teste em português: `deveCriarPassagemQuandoVagaDisponivel()`

## Perfis de usuário e permissões

| Perfil | Escopo |
|---|---|
| ADMIN_PLATAFORMA | Tudo (acesso direto ao banco, sem interface no MVP) |
| PROPRIETARIO | Seus veículos: financeiro, rotas, cobradores, configuração de horários |
| COBRADOR | Operação de um veículo: vender, lançar despesa, ver financeiro do veículo |
| MOTORISTA_AUTONOMO | Proprietário + cobrador da própria operação (perfil derivado, mesmas permissões da combinação) |

Perfis `ADMIN_COOPERATIVA` e `GESTOR_OPERACIONAL` modelados no DER mas **sem implementação no MVP** (entram com cooperativa em fase posterior).

## Visibilidade financeira por papel

Na tab Financeiro, a profundidade de visão muda conforme o papel do usuário em relação ao veículo selecionado:

**Proprietário do veículo (ou motorista autônomo no próprio veículo):**
- Visão completa: dados do dia + histórico de 7/30/mês/customizado
- Gráficos de tendência (receita 7 dias, receita vs despesa no histórico)
- Distribuição de despesas por categoria (donut)
- Pode excluir qualquer despesa, em qualquer data
- Pode editar despesas (futura)
- Botão "Consolidado" disponível para visão multi-veículo

**Cobrador puro (vinculado ao veículo mas não proprietário):**
- Visão apenas do dia atual
- Sem acesso a histórico, sem gráficos de tendência
- Lista de despesas do dia direto na tela principal
- Pode excluir apenas despesas que ele mesmo lançou
- Exclusão limitada ao mesmo dia do lançamento
- Botão "Consolidado" não aparece

Regra técnica: permissões são calculadas por combinação `usuario + veiculo`, não por perfil global. O mesmo usuário pode ser proprietário em um veículo (vê tudo) e cobrador em outro (visão limitada).

## O que NÃO está no MVP

Não implementar até que seja explicitamente pedido:
- Painel web (Fase 2)
- Funcionalidades de cooperativa (Fase 2)
- Cancelamento de passagem com estorno (Fase 2)
- Compra de passagem pelo passageiro (Fase 2 — projeto separado)
- Login com Google/OAuth (Fase 2)
- Cancelamento de conta de usuário pela interface (manual no MVP)
- NFC e maquininha (Fase 2)
- Email transacional (Fase 2)
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
- Ao trabalhar em features de UI mobile, consultar `docs/design/` para mapa de navegação e wireframes