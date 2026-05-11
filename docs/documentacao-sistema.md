# Documentação do Sistema CoopTicket

Conteúdo a ser populado a partir do documento original.

# Mudanças no plano de fases — virada para mobile-first

Este documento registra a decisão de pivotar o MVP para **mobile-first**, com cooperativa adiada para fase posterior. Aplicar no `docs/documentacao-sistema.md` substituindo as seções correspondentes.

## Substituir a seção "Direcionamento tecnológico"

### Direcionamento tecnológico

**MVP é mobile-first.** O foco inicial é entregar um app React Native completo que cobre o ciclo operacional, financeiro e administrativo do operador (proprietário, motorista autônomo, cobrador). Painel web fica para fase posterior, focado em administração de cooperativas.

**Justificativa**: operadores em campo passam o dia no celular. Um app completo entregue traz mais valor que web + mobile pela metade. Web vira valiosa quando entrarem cooperativas como clientes, com necessidades administrativas que mobile não atende bem.

**Stack:**

| Camada | Tecnologia | Motivo |
| --- | --- | --- |
| Back-end | Spring Boot 3.5 | Objetivo de aprendizado, robusto para APIs REST |
| Banco | PostgreSQL 15+ | Relacional, ideal para dados financeiros e controle de vagas com lock |
| App mobile | React Native (Expo) | Plataforma única para iOS/Android, ecossistema maduro |
| Autenticação | Spring Security + JWT | Padrão do mercado com Spring |
| Pagamentos | EFÍ / Pagar.me | EFÍ tem SDK Java excelente para Pix |
| Comprovante | PNG via WhatsApp share intent | Sem custo, sem burocracia, funciona universalmente |
| Hospedagem | Railway (back-end + Postgres) | Gratuito para começar |

**Painel web (Fase 2+)**: React + Vite, hospedado na Vercel. Foco em administração de cooperativa, relatórios consolidados pesados, e gestão da plataforma.

## Substituir o "Plano de Fases"

### Fase 0 — Fundação (2–3 semanas) — em andamento

**Objetivo:** Estrutura do projeto antes de qualquer regra de negócio.

**Tarefas:**
- Setup Spring Boot com JWT e perfis ✓
- Modelagem do banco com `veiculo_id` como chave central
- Configuração Docker + PostgreSQL
- Setup React Native com Expo
- Autenticação base (cadastro, login, refresh token)

**Entregável:** Login funcionando, navegação base do app.

---

### Fase 1A — Cadastro de veículos e onboarding (2–3 semanas)

**Objetivo:** Usuário entra no app e tem propósito claro.

**Tarefas:**
- Tela de propósito após login (sou cobrador / sou dono)
- CRUD de veículos no app (modos: linha_regular, sob_demanda, taxi)
- Geração de código de convite pelo proprietário
- Inserção de código pelo cobrador para vincular a um veículo
- Soft delete de cobrador com preservação de histórico
- Tela de seleção de veículo quando usuário tem múltiplos vínculos

**Entregável:** Proprietário cadastra veículo, gera código, cobrador insere código e vincula. Ambos veem o veículo na sua lista.

**Dependências:** Fase 0.

---

### Fase 1B — Horários e viagens (2–3 semanas)

**Objetivo:** Configurar grade de horários e gerar viagens diárias.

**Tarefas:**
- CRUD de `horario_template` no app (vinculado direto ao veículo)
- Job agendado que gera `viagem_instancia` para os próximos 7 dias
- Tela de viagens do dia no app
- Para modo `SOB_DEMANDA` e `TAXI`: criação manual de viagem ad-hoc
- Encerramento manual de viagem com geração de snapshot
- Job de encerramento automático de viagens vencidas (24h)

**Entregável:** Cobrador vê viagens programadas, abre uma, opera dentro dela, encerra ao final.

**Dependências:** Fase 1A.

---

### Fase 1C — Operação de passagens com Pix (3–4 semanas)

**Objetivo:** Cobrador vende passagens dentro de viagem ativa.

**Tarefas:**
- Tela "Emitir passagem" com origem/destino opcionais, número de passagens, valor editável (com sugestão da viagem), forma de pagamento (Pix/dinheiro)
- Geração de QR Code Pix dinâmico via EFÍ
- Webhook do EFÍ para confirmação automática
- Polling de fallback a cada 30s
- Decremento de vagas com lock otimista (`@Version`)
- Overbooking permitido com confirmação dupla na interface
- Sinalização visual de overbooking (badge + cor no card da viagem)
- Listagem de passagens vendidas
- Reenvio de comprovante (caso passageiro tenha perdido)

**Entregável:** Cobrador vende passagem, passageiro paga via Pix, sistema confirma e atualiza vagas.

**Dependências:** Fase 1B.

---

### Fase 1D — Comprovante via WhatsApp (1–2 semanas)

**Objetivo:** Emitir e enviar comprovante.

**Tarefas:**
- Geração assíncrona do comprovante em PNG (formato compatível com impressão térmica futura)
- Botão "enviar via WhatsApp" usando share intent nativo
- Opção alternativa "salvar imagem"
- Layout do ticket: nome do operador, placa do veículo, rota, data/hora, valor, identificador único, QR Code de validação

**Entregável:** Após confirmação da venda, cobrador toca em "enviar comprovante" e WhatsApp abre com a imagem pronta para escolher contato.

**Dependências:** Fase 1C.

---

### Fase 1E — Despesas e painel financeiro mobile (2–3 semanas)

**Objetivo:** Controle financeiro por veículo no próprio app.

**Tarefas:**
- Tela "Resumo do dia" com receita, despesas, lucro líquido
- Lançamento de despesas com categoria, valor, descrição
- Histórico financeiro com filtro por período (dia, semana, mês)
- Visualização de viagens fechadas (snapshot)
- Lista de despesas do veículo
- Para proprietário com múltiplos veículos: visão consolidada simples no header

**Entregável:** Proprietário/cobrador vê o financeiro completo do veículo direto no celular.

**Dependências:** Fase 1C.

---

### Fase 2 — Painel web + cooperativas + funcionalidades adicionais (8–10 semanas)

**Objetivo:** Atender cooperativas como cliente e expandir funcionalidades.

**Tarefas:**
- Painel web React para administração de cooperativa
- Implementação efetiva da hierarquia cooperativa → veículos
- Webhook de integração com sistema externo da cooperativa
- App separado para passageiros comprarem passagens online
- Cancelamento de passagem com estorno via API do gateway
- Login com Google (OAuth)
- Cancelamento de conta com fluxo LGPD-compliant
- Email transacional (SendGrid ou similar)
- Painel administrativo da plataforma (gestão de clientes, planos)

**Dependências:** Fase 1 completa.

---

### Fase 2.5 — Melhorias operacionais

- Impressão térmica do comprovante (Bluetooth, SDK Epson)
- Pagamento por aproximação NFC (SDK Stone/Cielo)
- Lista de passageiros e fretes pré-agendados (uso de taxistas)

---

### Fase 3 — Rastreamento em tempo real (4–6 semanas)

- Coleta de GPS no app do motorista
- WebSocket para envio de posição
- Mapa em tempo real no painel web
- Histórico de rotas percorridas

---

## Atualização da seção "Riscos e recomendações"

Adicionar:

**Risco específico do mobile-first**: o app precisa cobrir cenários complexos de configuração que normalmente seriam mais confortáveis em desktop (templates de horário, gestão de cobradores). Mitigação: simplificar formulários ao máximo, usar wizards quando necessário, focar em fluxos lineares.

**Risco de cooperativa adiada**: o esquema de banco já contempla cooperativa, mas como não vamos usar no MVP, há risco de descobrir problemas só quando ativarmos. Mitigação: manter a modelagem como está, testar cenários "com cooperativa" via dados de seed em ambiente de teste.