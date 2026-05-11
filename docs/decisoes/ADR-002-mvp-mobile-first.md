# ADR-002: MVP mobile-first com cooperativa adiada

## Status
Aceito — 2026-05-02

## Contexto

O plano original previa duas frentes de desenvolvimento em paralelo: painel web (React) para administração e relatórios pesados, app mobile (React Native) para operação em campo. Ambos consumindo a mesma API Spring Boot.

Durante o desenho da arquitetura de informação do app mobile, ficou claro que o operador (cobrador, motorista autônomo, proprietário-operador) precisa de funcionalidades que originalmente seriam apenas no painel web: dashboard financeiro, configuração de horários, gestão de cobradores. Limitar essas funções ao painel web significaria que o operador (público principal do MVP) não conseguiria operar de forma autônoma sem desktop.

Adicionalmente, cooperativas como cliente foram identificadas como **público secundário** que entra em fase posterior — não há urgência de servir cooperativa no MVP. Operadores autônomos (taxistas, motoristas individuais, proprietários de pequenos veículos) são autossuficientes e não dependem de cooperativa para operar.

## Decisão

**Pivotar para MVP mobile-first.**

1. App React Native cobrirá o ciclo completo do operador: operação, financeiro, configuração de veículos, configuração de horários, gestão de cobradores.
2. Painel web fica adiado para Fase 2, focado em administração de cooperativas e relatórios consolidados pesados.
3. Funcionalidades de cooperativa são **completamente ignoradas no MVP**. Campo `cooperativa_id` no banco continua existindo (sem mudança de schema), mas fica sempre `null`. Templates de horário ficam vinculados diretamente ao veículo.

## Alternativas consideradas

**Alternativa A — Manter desenvolvimento paralelo (web + mobile)**: foi descartada pelo risco de não terminar nenhuma das frentes sendo dev solo. Energia dividida costuma resultar em dois produtos pela metade.

**Alternativa B — Web-first**: foi descartada porque o operador (público principal) opera em campo no celular. Forçar uso de desktop seria fricção real para adoção.

**Alternativa C — Mobile-first com web administrativa mínima**: foi considerada mas descartada para o MVP. Web administrativa requer desenhar admin da plataforma, painel de cooperativa, autenticação separada — complexidade que não traz valor enquanto não houver clientes-cooperativa.

## Consequências

### Positivas

- **Foco em uma frente**: dev solo consegue entregar produto completo
- **Velocidade de entrega**: MVP completo possivelmente em metade do tempo
- **Alinhamento com realidade do operador**: ele já vive no celular
- **Portfólio coeso**: app funcional ponta-a-ponta vale mais que duas metades

### Negativas

- **UX comprometida em telas complexas**: configurar template com vigência, dias da semana, vagas e preço é menos confortável em mobile que em desktop. Mitigação: simplificar formulários ao máximo, usar wizards lineares.
- **Relatórios financeiros consolidados sofrem**: visão de múltiplos veículos com gráficos comparativos fica limitada em mobile. Mitigação: relatórios pesados ficam para o painel web futuro; mobile mostra resumos.
- **Portfólio técnico perde uma frente**: dev mostra back + mobile, sem front web. Mitigação: a Fase 2 traz o painel web depois — não é "nunca", é "depois".

### Neutras

- **Esquema de banco continua o mesmo**: cooperativa modelada mas inativa. Quando lançar Fase 2, basta começar a popular o campo. Sem migração necessária.
- **API pública continua igual**: endpoints projetados pensando em múltiplos clientes (web, mobile, futuro app de passageiro). Mobile-first não afeta o design da API.

## Documentos afetados

- `CLAUDE.md` — atualizado com a nova estratégia
- `docs/documentacao-sistema.md` — plano de fases reorganizado
- `docs/diagramas/arquitetura-containers.mermaid` — painel web marcado como "Fase 2"
- Próximos diagramas de design (mapa de navegação, wireframes) — focados exclusivamente em mobile

## Reavaliação

Esta decisão pode ser revisitada quando:

- Aparecer cliente-cooperativa real demandando administração centralizada (gatilho para acelerar painel web)
- Fase 1 completa estiver em uso por operadores reais (validar se mobile realmente atende)
- Houver capacidade de equipe (não-solo) para abrir frente paralela