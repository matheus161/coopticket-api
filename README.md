# CoopTicket

API Spring Boot para gestão de passagens de transporte intermunicipal. Suporta cooperativas de ônibus, táxis e motoristas autônomos com modelo centrado no veículo.

## Stack
- Java 21, Spring Boot 3.5.14
- PostgreSQL 15 + Flyway
- Spring Security + JWT
- EFÍ Pix

## Documentação
- [Documentação completa](docs/documentacao-sistema.md)
- [Modelo de dados](docs/der-decisoes.md)
- [Arquitetura](docs/arquitetura-decisoes.md)
- [Fluxos de venda](docs/sequencia-decisoes.md)
- [Decisões arquiteturais (ADRs)](docs/decisoes/)

## Como rodar localmente

### Pré-requisitos
- Java 21
- Docker e Docker Compose
- Maven (já incluído via wrapper)

### Passos

1. Copie o arquivo de variáveis de ambiente:
```
cp .env.example .env
```

2. Suba o banco de dados:
```
docker compose up -d
```

3. Aguarde o banco ficar saudável (cerca de 10 segundos):
```
docker compose ps
```

4. Rode a aplicação:
```
./mvnw spring-boot:run
```

5. Verifique se está funcionando:
```
curl http://localhost:8080/api/v1/health
```

Resposta esperada:
```json
{"status":"UP","service":"coopticket-api","database":"connected"}
```

### Comandos úteis

- Parar o banco: `docker compose down`
- Parar o banco e apagar dados: `docker compose down -v`
- Ver logs do banco: `docker compose logs -f postgres`
- Acessar o banco via psql: `docker exec -it coopticket-postgres psql -U coopticket`
