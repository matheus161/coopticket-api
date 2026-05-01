# Arquitetura de Containers — C4 Nível 2

Este documento descreve a arquitetura do sistema no nível de **containers** (aplicações deployáveis e bancos de dados). Acompanha o arquivo `arquitetura-containers.mermaid` (renderiza no GitHub) e o SVG visual gerado durante o planejamento.

## Visão geral

O sistema é composto por **5 containers internos** e se comunica com **3 sistemas externos** e **4 tipos de pessoas**.

```
[Pessoas] → [Painel Web | App Mobile] → [API Spring Boot] → [PostgreSQL]
                                            ↓ ↑
                                       [Job Agendado]
                                            ↓ ↑
                              [Gateway Pix | Sistema Coop | WhatsApp]
```

Princípio arquitetural central: **back-end único, dois clientes, jobs no mesmo processo**. Não há microsserviços, não há filas separadas, não há cache distribuído. Para um MVP com ~100 veículos, isso é mais que suficiente — e é o que cabe em um portfólio sem virar over-engineering.

## Containers internos

### 1. Painel Web

**Stack**: React 18+ com Vite, hospedado na Vercel.
**Porta de desenvolvimento**: 5173.
**Responsabilidade**: interface administrativa para uso em desktop.

Quem usa:
- Proprietário (configura veículos, vê financeiro consolidado)
- Admin de cooperativa (cria templates de horário, gerencia rotas)
- Admin da plataforma (Fase 2 — gerencia clientes e planos)

**O que faz**: telas de cadastro, dashboards com gráficos, relatórios exportáveis, configuração de webhook, geração de código de convite para cobradores.

**O que não faz**: nenhuma lógica de negócio — toda decisão é validada na API. Não acessa banco diretamente.

### 2. App Mobile

**Stack**: React Native com Expo.
**Build**: APK Android (foco inicial), iOS na Fase 2.
**Responsabilidade**: operação em campo.

Quem usa:
- Cobrador (vinculado a um veículo via código de convite)
- Motorista autônomo (proprietário + cobrador da própria operação)

**O que faz**: lista as viagens do dia do veículo, registra venda de passagem, gera QR Code Pix, confirma pagamento, envia comprovante via WhatsApp share intent, lança despesas.

**Decisão de design**: o app **não funciona offline no MVP**. Vendas exigem conexão para gerar o Pix. Em campo sem internet, fica como melhoria futura (sincronização posterior).

### 3. API Back-end

**Stack**: Spring Boot 3.x, Java 21, Spring Security com JWT, Spring Data JPA, Flyway para migrations.
**Porta de desenvolvimento**: 8080.
**Hospedagem**: Railway (gratuito para começar).
**Responsabilidade**: regras de negócio, persistência, integrações.

Módulos internos previstos (organização de pacotes, não containers separados):

```
br.com.passagens
├── auth          → JWT, login, refresh token
├── usuario       → CRUD de usuários, perfis, vínculos
├── veiculo       → CRUD de veículos, modos de operação
├── cooperativa   → CRUD de cooperativa, rotas catálogo
├── horario       → templates, exceções, geração de instâncias
├── viagem        → CRUD de viagem_instancia, fechamento
├── passagem      → venda, cancelamento, listagem
├── pagamento     → integração EFÍ/Pagar.me, webhook Pix, polling
├── financeiro    → snapshot, relatórios, dashboards
├── despesa       → CRUD de despesas
├── webhook       → endpoint de integração com cooperativa, log
├── comprovante   → geração de PNG do ticket
└── infra         → configurações, exception handlers, util
```

**O que expõe**: API REST em `/api/v1/*`, webhook receiver em `/api/v1/integracao/*`, Swagger/OpenAPI em `/swagger-ui.html`.

**O que consome**: PostgreSQL via JDBC, EFÍ/Pagar.me via HTTPS.

### 4. PostgreSQL

**Versão**: 15 ou superior.
**Porta**: 5432.
**Hospedagem**: instância gerenciada do Railway (mesma plataforma da API para evitar latência de rede).

**Por que PostgreSQL e não outro banco**: o domínio é fortemente relacional (veículo tem viagens, viagens têm passagens, passagens têm pagamentos), tem requisito de consistência financeira (transações ACID importam) e o sistema usa lock otimista via `@Version`. PostgreSQL atende tudo isso e tem suporte nativo a `jsonb` (usado em `webhook_log.payload_recebido` e `evento_auditoria`).

**Não usa**: replicação, particionamento, sharding. Para o volume previsto, instância única basta.

### 5. Job Agendado

**Stack**: Spring `@Scheduled` no mesmo processo da API.
**Não é um container separado** — está dentro do JAR da API, mas conceitualmente vale destacar porque tem lifecycle próprio.

Tarefas agendadas:

| Job | Frequência | Responsabilidade |
|---|---|---|
| Gerar instâncias de viagem | Diário 03:00 | Cria `viagem_instancia` para os próximos 7 dias a partir dos templates ativos |
| Fechar viagens vencidas | A cada 1h | Encerra viagens com `partida_prevista < now() - 24h` ainda em estado PROGRAMADA, gera snapshot |
| Expirar Pix pendentes | A cada 5min | Marca `pagamento_pix` com `expira_em < now()` como EXPIRADO e a passagem associada como EXPIRADA |
| Limpar logs antigos | Semanal domingo 02:00 | Remove `webhook_log` com mais de 90 dias |

**Por que no mesmo processo**: simplicidade. Em escala maior viraria um worker separado (ex: usando Quartz ou um serviço dedicado), mas para MVP é desnecessário.

## Sistemas externos

### Gateway de pagamento (EFÍ ou Pagar.me)

**Recomendação**: começar com EFÍ — tem SDK Java oficial, ambiente de homologação fácil, taxas Pix competitivas, e é brasileira (suporte em português).

**Comunicação**: HTTPS bidirecional.
- API → Gateway: criação de cobrança Pix dinâmica.
- Gateway → API: webhook quando pagamento é confirmado.
- API → Gateway: polling de fallback (consulta status quando webhook atrasa mais de 30 segundos).

**Resiliência**: implementar retry com backoff exponencial para chamadas de criação. Webhook do gateway deve ter endpoint público (ex: `https://api.minhaplataforma.com.br/api/v1/webhooks/pix`).

### Sistema da Cooperativa (Fase 1F)

**Tipo**: sistema legado externo que **continua existindo em paralelo**. Cada cooperativa tem o seu, geralmente é um sistema desktop ou web próprio.

**Comunicação**: webhook unidirecional (cooperativa → nossa API). A cooperativa **chama nosso endpoint** quando vende uma passagem em guichê.

**Endpoint**: `POST /api/v1/integracao/passagem-guiche`.
**Autenticação**: token específico do veículo (gerado pelo proprietário no painel) enviado em `Authorization: Bearer <token>`.
**Idempotência**: por `id_externo` no payload — chamadas duplicadas não criam passagens duplicadas.

A cooperativa é responsável por implementar a chamada do lado dela. Nós fornecemos: documentação Swagger do endpoint, exemplos de payload, ambiente de homologação.

### WhatsApp (via share intent)

**Não é integração de API** — é uso do compartilhamento nativo do sistema operacional do celular. O app gera a imagem do comprovante e abre o seletor de apps do Android/iOS, com WhatsApp já como opção.

**Por que não usar a API oficial do WhatsApp Business**: custa por mensagem, exige aprovação de templates pela Meta, exige número verificado. Para MVP é exagero. Share intent é grátis, instantâneo e funciona.

**Limitação**: depende do cobrador ter WhatsApp instalado. Se não tiver, o app oferece opção de salvar a imagem localmente ou compartilhar por outro app.

## Pontos importantes da arquitetura

### Por que back-end único e não microsserviços

Microsserviços fazem sentido quando você tem times separados, escalas muito diferentes entre módulos, ou requisito de deploy independente. Nada disso se aplica aqui. Para um sistema com ~10 entidades centrais e um time pequeno (provavelmente solo no início), monolito é a escolha certa. Pode ser modularizado internamente (pacotes bem definidos), mas continua sendo um único processo.

Migração para microsserviços, se vier, seria orgânica: extrair primeiro o módulo de pagamentos (que tem regras de retry, webhook, e crescerá), depois o de relatórios. Mas isso é problema de daqui a 1 ano, não de hoje.

### Por que jobs no mesmo processo

Mesma razão. Spring `@Scheduled` resolve perfeitamente o caso de uso. Se em algum momento precisar de processamento distribuído, evolui para Quartz com cluster ou sai os jobs para um worker separado.

### Como a autenticação flui

1. Usuário faz login no painel ou app, envia email/senha para `POST /api/v1/auth/login`.
2. API valida, retorna **access token** (JWT, 15 minutos) e **refresh token** (90 dias, armazenado em banco).
3. Cliente armazena tokens (mobile: SecureStore; web: httpOnly cookie ou memória).
4. Cada requisição enviada com `Authorization: Bearer <access_token>`.
5. Spring Security valida assinatura, extrai `usuario_id` e perfis do JWT.
6. Quando access token expira, cliente usa refresh token em `POST /api/v1/auth/refresh` para obter novo par.

**Webhooks têm autenticação separada**: token estático por veículo (sem JWT), validado no controller específico.

### Onde mora o estado

Todo estado durável fica no PostgreSQL. **Não há cache** no MVP (Redis, Hazelcast, etc.). Sessões são stateless via JWT — o servidor não guarda nada sobre quem está logado.

Estado transiente do cliente (formulários abertos, lista filtrada na tela) fica no React Query / TanStack Query no front, que faz cache de respostas da API e revalidação. No mobile, mesmo padrão.

### Limites do MVP que valem documentar

- **Sem multi-tenancy real**: por enquanto cada usuário vê só os seus dados via `proprietario_id`, mas não há isolamento físico entre clientes da plataforma.
- **Sem rate limiting**: API confia que clientes legítimos não vão abusar. Para Fase 2 (plataforma aberta), rate limiting passa a ser obrigatório.
- **Sem observabilidade avançada**: logs estruturados via SLF4J/Logback no Spring, mas nada de Prometheus, Grafana, OpenTelemetry. Para começar, logs do Railway resolvem.
- **Sem CDN no front**: Vercel já entrega via CDN próprio dela, então isso é automático no painel. Imagens de comprovante (PNG) ficariam idealmente em S3 ou similar — para MVP podem ficar no próprio Postgres como bytea ou em storage do Railway.

## Containers de Fase 2 e 3 (previsão)

| Container futuro | Quando entra | Substitui ou adiciona |
|---|---|---|
| App Passageiro (React Native) | Fase 2 | Adiciona — compra de passagens online |
| Maquininha NFC integrada | Fase 2 | Adiciona ao app mobile do cobrador (SDK) |
| Serviço de Localização | Fase 3 | Adiciona — coleta GPS via WebSocket |
| Impressora térmica | Fase 2.5 | Adiciona ao app mobile (SDK Bluetooth) |
| Mapa em tempo real (web) | Fase 3 | Componente novo no painel web |

Nenhum desses introduz novo serviço de back-end — todos consomem a mesma API. O Serviço de Localização adiciona um **endpoint WebSocket** ao back-end existente, não um processo separado.

## Hospedagem e deploy (estado MVP)

| Container | Onde roda | Custo inicial |
|---|---|---|
| Painel Web (React) | Vercel | Grátis |
| App Mobile | Build local com EAS, distribuído via APK ou TestFlight | Grátis no início |
| API Spring Boot | Railway (container Docker) | Grátis até consumir cota |
| PostgreSQL | Railway (instância gerenciada) | Grátis até cota |
| Imagens de comprovante | Railway storage ou bytea no Postgres | Grátis |

Domínio próprio (ex: `minhaplataforma.com.br`) é o único custo real obrigatório no início — em torno de R$ 40/ano no registro.br.

## Como evoluir

Quando o MVP provar valor e o número de veículos passar de ~100 ativos:

1. **Separar storage de comprovantes** para S3 / Cloudflare R2 — bytea no banco escala mal.
2. **Adicionar Redis** para cache de consultas frequentes (lista de viagens do dia, perfis de usuário no JWT).
3. **Mover jobs para worker separado** quando a geração de instâncias começar a competir com requests.
4. **Observabilidade**: Sentry para erros, OpenTelemetry para traces distribuídos quando aparecerem mais containers.

Nada disso entra no MVP. Documentar essa evolução no README é mais valioso para portfólio do que implementar prematuramente.
