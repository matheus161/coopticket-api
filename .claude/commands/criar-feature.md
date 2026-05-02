# Criar nova feature

Você vai criar uma nova feature seguindo o padrão package-by-feature do projeto CoopTicket. Antes de começar, leia `CLAUDE.md` e `docs/der-decisoes.md` para garantir que está alinhado com as convenções.

## Argumentos esperados
- Nome da feature em português, singular, minúsculo (ex: `veiculo`, `rota`, `passagem`)
- Descrição breve do que a feature faz

Se o usuário não passou esses argumentos, pergunte antes de continuar.

## Passos a executar

### 1. Validar convenções
- Nome deve estar em minúsculo, singular, sem acentos
- Não deve já existir em `src/main/java/br/com/coopticket/<nome>/`
- Se já existir, pare e avise o usuário

### 2. Criar estrutura de pastas
Em `src/main/java/br/com/coopticket/<nome>/` criar subpastas:
- `controller/`
- `service/`
- `repository/`
- `domain/`
- `dto/`

Em `src/test/java/br/com/coopticket/<nome>/` criar a mesma estrutura para testes.

### 3. Criar arquivos esqueleto

**Entidade JPA** em `domain/<Nome>.java` (com inicial maiúscula):
- Anotada com `@Entity`, `@Table(name = "<nome>")`
- ID UUID com `@Id @GeneratedValue`
- Campo `criadoEm` LocalDateTime com `@CreationTimestamp`
- Campo `atualizadoEm` LocalDateTime com `@UpdateTimestamp`
- Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`

**Repository** em `repository/<Nome>Repository.java`:
- Interface estendendo `JpaRepository<<Nome>, UUID>`
- Sem métodos customizados ainda

**Service** em `service/<Nome>Service.java`:
- Anotado com `@Service`, `@Transactional(readOnly = true)`
- Construtor recebendo o repository (injeção via construtor)
- Métodos vazios: `listar()`, `buscarPorId(UUID id)`, `criar()`, `atualizar()`, `deletar()`
- Cada método com comentário `// TODO: implementar`

**DTOs** em `dto/`:
- `<Nome>Request.java` como `record` (Java 21)
- `<Nome>Response.java` como `record`
- Campos vazios por enquanto, apenas estrutura

**Controller** em `controller/<Nome>Controller.java`:
- Anotado com `@RestController`, `@RequestMapping("/api/v1/<nome>s")` (plural)
- Endpoints: `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`
- Cada endpoint chamando o service correspondente
- DTOs como entrada/saída (nunca expor a entidade)
- Validação `@Valid` nos POST e PUT

### 4. Criar migration Flyway
Em `src/main/resources/db/migration/`, criar arquivo seguindo o padrão de versionamento.

Verifique a maior versão existente (ex: V3__) e crie a próxima (V4__criar_<nome>.sql).

Conteúdo template:
```sql
-- Migration: Criar tabela <nome>
-- Feature: <descrição passada pelo usuário>

CREATE TABLE <nome> (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
    -- TODO: adicionar colunas específicas da feature
);

CREATE INDEX idx_<nome>_criado_em ON <nome>(criado_em);
```

### 5. Criar testes esqueleto
- `<Nome>ServiceTest.java` com `@DataJpaTest` e Testcontainers
- `<Nome>ControllerTest.java` com `@WebMvcTest`
- Testes vazios com `@Test` e `// TODO: implementar`

### 6. Reportar
Mostre ao usuário:
- Lista de arquivos criados
- Próximos passos sugeridos (ex: "Adicione os campos específicos na entidade e migration, depois implemente os métodos do service")
- Lembre que precisa rodar `./gradlew build` para validar que compila
