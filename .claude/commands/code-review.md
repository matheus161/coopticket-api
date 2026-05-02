# Code Review CoopTicket

Você vai revisar as alterações recentes (não commitadas) seguindo as convenções do projeto. Faça uma análise crítica e construtiva.

## Antes de começar
- Leia `CLAUDE.md` para relembrar convenções
- Identifique quais arquivos foram alterados via `git status` e `git diff`

## Checklist de verificação

Para cada arquivo alterado, verifique e reporte:

### Estrutura e organização
- [ ] Está em `br.com.coopticket.<modulo>/<subpasta>/` correto?
- [ ] Module só importa de outros módulos via Service público (nunca Repository de outro módulo)?
- [ ] Nome do arquivo corresponde à classe?

### Convenções de código
- [ ] DTOs são `record` (Java 21)?
- [ ] Entidades JPA não são expostas em controllers (apenas DTOs)?
- [ ] Nomes em português para domínio (`Veiculo`, não `Vehicle`)?
- [ ] Termos técnicos em inglês (`Repository`, `Service`, `Controller`)?
- [ ] Validação Bean Validation (`@Valid`, `@NotNull`, etc.) presente em DTOs de entrada?
- [ ] `Optional` apenas em retorno, nunca em parâmetro?

### Qualidade e padrões
- [ ] Injeção de dependência via construtor (não `@Autowired` em campo)?
- [ ] Logs com SLF4J em pontos importantes?
- [ ] Exceções customizadas estendem `BusinessException` (não `RuntimeException` genérica)?
- [ ] `@Transactional` aplicado corretamente nos services (readOnly por padrão)?
- [ ] Open-in-view não está sendo abusado (lazy loading dentro de transação)?

### Banco de dados
- [ ] Mudança em entidade tem migration Flyway correspondente?
- [ ] Migration tem versionamento correto (V<numero>__)?
- [ ] Migration usa `IF NOT EXISTS` ou é segura para reexecução?
- [ ] Índices adequados criados?
- [ ] Constraints CHECK aplicadas onde fazem sentido?

### Segurança
- [ ] Endpoint protegido por role correta via Spring Security?
- [ ] Dados sensíveis não vão em logs?
- [ ] Validação de entrada presente?

### Regras críticas do CoopTicket
- [ ] Operações em vagas usam lock otimista (`@Version`)?
- [ ] Webhooks têm idempotência por `id_externo`?
- [ ] Vagas só decrementam quando passagem CONFIRMA (não na criação)?
- [ ] Chamadas a serviços externos (EFÍ) estão FORA de transação de banco?
- [ ] Comprovantes/imagens geradas de forma assíncrona?

### Testes
- [ ] Mudanças relevantes têm testes correspondentes?
- [ ] Nomes de teste em português descritivo?
- [ ] Testes de integração usam Testcontainers?

## Formato do relatório

Estruture sua resposta assim:

**Resumo**: [✅ Aprovado / ⚠️ Aprovado com ressalvas / ❌ Precisa ajustes]

**Pontos positivos**:
- [lista]

**Problemas encontrados**:
- [arquivo:linha] descrição do problema, sugestão de correção

**Sugestões opcionais**:
- [melhorias que não bloqueiam mas valem considerar]

**Próximos passos**:
- [o que fazer antes de commitar]

Seja direto e construtivo. Não inflar o relatório com elogios genéricos.
