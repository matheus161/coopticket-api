# ADR-001: Organização do código em package-by-feature

## Status
Aceito

## Contexto
Precisamos definir como organizar os pacotes Java do projeto. As alternativas são organização por camada técnica (`controllers/`, `services/`, `repositories/`) ou por feature de negócio (`veiculo/`, `passagem/`, etc.).

## Decisão
Adotar **package-by-feature**. Cada feature de negócio terá seu próprio pacote raiz contendo subpacotes `controller`, `service`, `repository`, `domain`, `dto`.

## Consequências
**Positivas**: limites claros entre features, fácil localização de código relacionado, prepara para evolução para Spring Modulith ou microsserviços.

**Negativas**: requer disciplina manual — sem ferramenta forçando os limites, depende de convenção do time.

**Mitigação**: regra de ouro de comunicação inter-módulos via service público apenas (nunca acessar repository de outro módulo).
