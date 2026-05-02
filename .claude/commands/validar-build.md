# Validar Build

Execute a sequência completa de validação do projeto antes de commit. Reporte cada etapa.

## Passos a executar (em ordem)

### 1. Verificar formatação
```bash
./gradlew spotlessCheck
```
Se falhar, sugira rodar `./gradlew spotlessApply` para corrigir automaticamente. Se a tarefa não existir, ignore essa etapa e avise que Spotless não está configurado.

### 2. Compilar projeto
```bash
./gradlew build -x test
```
Se falhar, mostre erros de compilação completos.

### 3. Rodar testes unitários
```bash
./gradlew test
```
Mostre resumo de testes passados/falhados. Se houver falhas, mostre os testes que falharam.

### 4. Verificar que aplicação sobe
- Verifique se o Postgres está rodando: `docker compose ps`
- Se não estiver, sugira `docker compose up -d`
- Tente subir a aplicação em background por 30 segundos
- Faça `curl http://localhost:8080/api/v1/health`
- Verifique se retorna status UP

### 5. Verificar migrations Flyway
- Liste arquivos em `src/main/resources/db/migration/`
- Confirme que versionamento está sequencial sem gaps (V1, V2, V3, ...)
- Confirme que nomes seguem padrão `V<n>__<descricao_em_snake_case>.sql`

## Relatório final

Apresente um sumário:
```
Validação do Build — CoopTicket
✅/❌ Formatação
✅/❌ Compilação
✅/❌ Testes (X/Y passados)
✅/❌ Aplicação sobe
✅/❌ Migrations consistentes

Status geral: PRONTO PARA COMMIT / PRECISA AJUSTES
```

Se algo falhar, sugira ações corretivas específicas.
