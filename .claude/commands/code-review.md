# Code Review CoopTicket

Você vai revisar as alterações recentes (não commitadas) seguindo as convenções do projeto. Faça uma análise crítica e construtiva.

## Antes de começar
- Leia `CLAUDE.md` para relembrar convenções
- Consulte documentos relevantes em `docs/` quando o contexto pedir (ex: `docs/der-decisoes.md` para mudanças de schema, `docs/sequencia-decisoes.md` para fluxos críticos)
- Identifique quais arquivos foram alterados via `git status` e `git diff`
- Entenda o escopo da mudança (qual módulo de feature está sendo afetado)

## Checklist de verificação

Para cada arquivo alterado, verifique e reporte:

### Estrutura e organização
- [ ] Está em `br.com.coopticket.<modulo>/<subpasta>/` correto?
- [ ] Módulo só importa de outros módulos via Service público (nunca Repository de outro módulo)?
- [ ] Nome do arquivo corresponde à classe?
- [ ] Endpoints versionados (`/api/v1/...`)?

### Convenções de código
- [ ] DTOs são `record` (Java 21)?
- [ ] Entidades JPA não são expostas em controllers (apenas DTOs)?
- [ ] Nomes em português para domínio (`Veiculo`, não `Vehicle`)?
- [ ] Termos técnicos em inglês (`Repository`, `Service`, `Controller`)?
- [ ] Validação Bean Validation (`@Valid`, `@NotNull`, `@Size`) presente em DTOs de entrada?
- [ ] `Optional` apenas em retorno, nunca em parâmetro?
- [ ] Variáveis booleanas com verbos (`estaAtivo`, `temVagas`, `podeVender`)?
- [ ] Constantes ao invés de números mágicos (`public static final` ou enums)?
- [ ] Sem retorno de `null` em métodos de coleção (usar coleção vazia)?
- [ ] Pattern matching e switch expressions onde aplicável?

### Qualidade e padrões
- [ ] Injeção de dependência via construtor (não `@Autowired` em campo)?
- [ ] Logs com SLF4J em pontos importantes (sem `System.out.println`)?
- [ ] Exceções customizadas estendem `BusinessException` (não `RuntimeException` genérica)?
- [ ] `@Transactional(readOnly = true)` por padrão nos services, métodos de escrita marcados explicitamente?
- [ ] Global exception handler mapeia para status HTTP corretos?
- [ ] `open-in-view: false` respeitado (sem lazy loading fora de transação)?

### Segurança
- [ ] Sem secrets ou credenciais hardcoded (usar `.env` e `@Value`)?
- [ ] Endpoint protegido por perfil correto via Spring Security (`@PreAuthorize`)?
- [ ] Validação de entrada presente em todos os endpoints (Bean Validation)?
- [ ] Prevenção de SQL injection (queries JPA, sem concatenação de string)?
- [ ] Dados sensíveis não vão em logs nem em respostas (senhas, tokens, CPF)?
- [ ] Assinaturas de webhook validadas (JWT do EFÍ, token da cooperativa)?
- [ ] Premissa HTTPS-only documentada para chamadas externas?
- [ ] Headers de segurança configurados quando aplicável?

### Performance
- [ ] Sem padrão N+1 nas queries (usar `@EntityGraph`, `JOIN FETCH` ou DTO projection)?
- [ ] Índices de banco apropriados (alinhados com `docs/der-decisoes.md`)?
- [ ] Paginação em endpoints de listagem (`Pageable`, com `max-page-size` aplicado)?
- [ ] Operações pesadas executadas de forma assíncrona (`@Async` para geração de imagens, notificações)?
- [ ] Chamadas HTTP externas FORA de transações de banco?
- [ ] Pool de conexões (HikariCP) com tamanho apropriado para o cenário?
- [ ] Cache aplicado onde faz sentido (e invalidado quando precisa)?

### Banco de dados
- [ ] Mudança em entidade tem migration Flyway correspondente?
- [ ] Migration tem versionamento correto e sequencial (`V<numero>__`)?
- [ ] Nome da migration segue padrão `V<n>__<descricao_em_snake_case>.sql`?
- [ ] Migration é idempotente ou usa `IF NOT EXISTS` quando aplicável?
- [ ] Foreign keys explícitas com estratégia `ON DELETE` definida?
- [ ] Constraints CHECK aplicadas onde regras de negócio exigem (enums, XOR de owner)?
- [ ] Índices adequados criados (alinhados com queries reais)?
- [ ] `@Version` em entidades com escrita concorrente (vagas, viagem)?

### Regras críticas do CoopTicket
- [ ] Vagas só decrementam quando passagem chega em CONFIRMADA (nunca na criação)?
- [ ] Webhooks têm idempotência por `id_externo` (constraint unique parcial)?
- [ ] Lock otimista (`@Version`) em updates de `viagem_instancia`?
- [ ] Chamadas a serviços externos (EFÍ) FORA de transação de banco?
- [ ] Geração de comprovante PNG é assíncrona (não bloqueia resposta do webhook)?
- [ ] Polling de fallback implementado para Pix quando webhook falha?
- [ ] Todos os timestamps em UTC, conversão de fuso só na apresentação?
- [ ] `veiculo_id` presente em todas as tabelas operacionais (consultar DER)?

### Testes
- [ ] Mudanças relevantes têm testes correspondentes?
- [ ] Testes unitários para métodos públicos de service?
- [ ] Testes de integração usam Testcontainers (Postgres real)?
- [ ] Padrão Arrange-Act-Assert seguido?
- [ ] Nomes de teste em português descrevendo comportamento (`deveCriarPassagemQuandoVagaDisponivel`)?
- [ ] Variáveis de teste com nomenclatura clara (`input`, `mock`, `actual`, `expected`)?
- [ ] Edge cases cobertos (overbooking, concorrência, Pix expirado)?
- [ ] Testes não dependem de ordem de execução?

### Legibilidade
- [ ] Métodos com no máximo 30 linhas (Java é mais verboso que TS)?
- [ ] Responsabilidade única por método?
- [ ] Early returns para evitar nesting profundo?
- [ ] Javadoc em services públicos e métodos complexos?
- [ ] Sem linhas em branco dentro de métodos?
- [ ] Nomes significativos (palavras completas, não abreviações)?
- [ ] Sem código comentado (usar histórico do git)?

### Documentação
- [ ] Decisões arquiteturais registradas como ADR em `docs/decisoes/`?
- [ ] Mudanças significativas refletidas nos `docs/` relevantes?
- [ ] Anotações OpenAPI nos controllers (`@Operation`, `@ApiResponse`)?
- [ ] README atualizado se instruções de setup/execução mudaram?

## Níveis de severidade

Classifique cada problema encontrado usando estes níveis:

- 🔴 **Crítico**: precisa corrigir antes do merge (vulnerabilidades de segurança, risco de perda de dados, bugs que quebram funcionalidade, violação de regras críticas do CoopTicket)
- 🟡 **Sugestão**: deveria ser corrigido (qualidade de código, manutenibilidade, bugs menores, violações de convenção)
- 🟢 **Bom ter**: melhorias opcionais ou refinamentos

## Formato do relatório

Estruture sua resposta assim:

**Resumo**: [✅ Aprovado / ⚠️ Aprovado com ressalvas / ❌ Precisa ajustes]

**Pontos positivos**:
- [lista do que está bem feito — sem genéricos]

**Problemas encontrados**:
- 🔴/🟡/🟢 [arquivo:linha] descrição do problema, sugestão de correção com exemplo de código quando útil

**Sugestões opcionais**:
- [melhorias que não bloqueiam mas valem considerar]

**Próximos passos**:
- [o que fazer antes de commitar/merge]

Seja direto e construtivo. Não inflar o relatório com elogios genéricos.