# Fluxos de Venda de Passagem — Documentação dos Diagramas de Sequência

Este documento acompanha os três diagramas de sequência em `sequencia-venda-passagem.mermaid` e explica as regras de negócio, decisões técnicas e armadilhas de implementação de cada fluxo.

## Por que três diagramas e não um

O fluxo de venda tem três caminhos com lógicas distintas:

1. **Caminho feliz via app**: cobrador vende, passageiro paga, webhook confirma. ~95% dos casos.
2. **Fallback de polling**: webhook do Pix não chega ou atrasa. ~5% dos casos, mas crítico de implementar.
3. **Venda via guichê externo**: sistema da cooperativa notifica nosso sistema. Fluxo de integração com regras próprias de idempotência.

Misturar tudo em um diagrama vira espaguete ilegível. Separados, cada um vira referência para o desenvolvedor durante a implementação da feature correspondente.

---

## Diagrama 1 — Caminho feliz da venda via app

### Visão geral

Cobrador toca em "vender passagem" → escolhe a viagem → app chama API → API gera Pix no EFÍ → cobrador mostra QR Code → passageiro paga → EFÍ chama nosso webhook → API confirma passagem → cobrador envia comprovante via WhatsApp.

### Pontos críticos de implementação

**Lock otimista na viagem**: ao buscar `viagem_instancia` para validar vagas, usamos `@Version` do JPA. Se duas requisições chegarem ao mesmo tempo tentando vender a vaga 40, apenas uma faz o UPDATE com sucesso — a outra recebe `OptimisticLockException` e o serviço deve retornar erro 409 ou retentar.

**Transação atômica**: criar passagem + criar pagamento_pix + chamar EFÍ é uma operação lógica única, mas **não pode ser uma única transação de banco**. Por quê? Porque a chamada ao EFÍ é HTTP — se você abrir transação, chamar EFÍ, e o EFÍ demorar 5 segundos, sua conexão de banco fica presa. Padrão recomendado:

1. Abrir transação curta: `INSERT passagem (status=AGUARDANDO_PAGAMENTO)` e commit.
2. Chamar EFÍ fora da transação.
3. Abrir nova transação: `INSERT pagamento_pix` (já com txid retornado) e commit.

Se o passo 2 falhar, a passagem fica órfã com status AGUARDANDO_PAGAMENTO e expira sozinha (job de limpeza). Sem inconsistência.

**Vagas só decrementam quando passagem CONFIRMA, não quando criam a passagem**. Isso evita "reservar" vaga para quem não vai pagar. Na prática:

- Status AGUARDANDO_PAGAMENTO: passagem existe mas vaga não foi descontada
- Status CONFIRMADA: passagem está paga e vaga foi descontada

A consequência: durante a janela de pagamento, **dois cobradores podem gerar Pix para a mesma vaga**. Se os dois passageiros pagarem, vira overbooking. Para o MVP, decisão consciente: aceitamos esse risco. Em escala maior, daria pra implementar reserva temporária com TTL.

**Validação de assinatura do webhook EFÍ**: o EFÍ envia um header `Authorization` com JWT assinado por chave assimétrica deles. **Não confie no payload sem validar**. Sem essa validação, qualquer um na internet poderia chamar `/webhooks/pix` dizendo "passagem X foi paga". Spring permite validar isso com filtro custom.

**Idempotência do webhook**: o EFÍ pode reenviar o webhook se receber timeout. Use o `txid` como chave: se o pagamento já está PAGO, o UPDATE não muda nada e a resposta é 200 OK normal. Não retorne erro em duplicatas — o EFÍ entende como falha e fica reenviando.

**Geração do PNG do comprovante**: feita **assíncrona** (não bloqueia a resposta do webhook ao EFÍ). Use `@Async` do Spring ou uma fila simples. O EFÍ tem timeout curto (3-5s) — se o webhook handler demorar gerando imagem, o EFÍ desiste e reenvia, criando confusão.

**Notificação ao app do cobrador**: implementação do MVP pode ser por **polling local no app** (a cada 5s consulta `GET /api/v1/passagens/{id}/status`) ao invés de push notification. Push exige Firebase configurado, certificado iOS, etc. — deixa pra depois. Polling de 5 segundos durante a janela de espera do Pix (5-10 minutos) não pesa.

### Erros que valem prever desde já

| Erro | Como tratar |
|---|---|
| EFÍ fora do ar ao criar Pix | Retorna 503 ao app, cobrador pode retentar |
| EFÍ retorna mas resposta não chega (timeout) | Marca passagem como AGUARDANDO_PAGAMENTO sem txid; job de limpeza expira após 10 min |
| Webhook Pix com txid inexistente | Loga em `webhook_log` com erro, retorna 200 (não 4xx — EFÍ reenviaria) |
| OptimisticLockException no UPDATE de vagas | Retentativa interna 1x; se falhar, retorna 409 ao app |
| Passageiro paga depois do `expira_em` | EFÍ rejeita o pagamento; nada chega ao webhook |

---

## Diagrama 2 — Fallback de polling

### Por que esse fluxo existe

Webhook é prática padrão, mas falha. Causas comuns: instabilidade de rede entre EFÍ e nosso servidor, deploy em andamento causando 502 transitório, IP do servidor mudou, certificado SSL renovou. Sem fallback, passageiro paga e cobrador nunca confirma — situação inaceitável.

### Como funciona

Job Spring `@Scheduled(fixedDelay=30000)` roda a cada 30 segundos:

1. Busca todos os `pagamento_pix` com status=PENDENTE criados há mais de 30s e cujo `ultimo_polling` foi há mais de 30s
2. Para cada um, chama `GET /v2/cob/{txid}` no EFÍ
3. Se retornar `CONCLUIDA`, processa exatamente como o webhook faria
4. Se ainda PENDENTE, atualiza `ultimo_polling` e segue
5. Se REMOVIDA_PELO_USUARIO_RECEBEDOR (expirou), marca passagem como EXPIRADA

### Cuidados

**Circuit breaker**: se o EFÍ está fora do ar, o polling vai bombardear a API deles com erros. Use Resilience4j ou um contador simples — após 5 falhas consecutivas, pausa 5 minutos antes de tentar de novo.

**Throttling**: limite a quantidade de polling por execução (ex: máximo 50 Pix por tick). Se houver 500 pendentes, processa em lotes para não sobrecarregar.

**Idempotência com webhook**: webhook pode chegar **depois** do polling ter resolvido. O UPDATE precisa ser cuidadoso:

```sql
UPDATE pagamento_pix
SET status = 'PAGO', webhook_recebido_em = now()
WHERE txid = ? AND status != 'PAGO'
```

Se a linha já está PAGO (resolvida pelo polling), o UPDATE afeta 0 linhas. Sem erro, sem duplicação de UPDATE em viagem.

**Janela mínima antes do primeiro polling**: o EFÍ tem latência típica de 2-5 segundos para registrar o webhook. Se você fizer polling antes disso, vai gastar requisições à toa. Daí a condição `criado_em < now() - 30s`.

**Limite por Pix**: cada Pix tem `expira_em` (geralmente 30 minutos). Após expirar, polling marca como EXPIRADO e para. Sem isso, polling de Pix antigos cresceria infinitamente.

---

## Diagrama 3 — Venda via guichê com webhook da cooperativa

### Por que esse fluxo é diferente

Aqui o **nosso sistema é o receptor do webhook**, não o gateway. A cooperativa tem sistema próprio de venda em guichê e nos avisa via HTTP. Implicações:

- Não há "criar Pix" — a venda já foi feita do lado da cooperativa, em dinheiro/cartão/Pix do sistema deles
- Não validamos pagamento, só registramos a venda
- Vagas precisam ser decrementadas **imediatamente** (passagem já está paga)
- Cobrador no app precisa ver a atualização rapidamente

### Pontos críticos

**Autenticação por token estático do veículo**: cada veículo tem um token gerado pelo proprietário no painel (`webhook_config.token_acesso`). O sistema da cooperativa envia esse token em `Authorization: Bearer`. Diferente do JWT dos usuários — esse token não expira, não tem refresh, é específico do canal de integração.

**Idempotência por `id_externo`**: o sistema da cooperativa envia um identificador próprio (ex: número da venda no sistema legado). Esse id vai para `passagem.id_externo` com constraint UNIQUE parcial. Se a cooperativa reenviar (timeout, retry), a query `SELECT passagem WHERE id_externo='VND-9981'` retorna o registro existente e a API responde 409 sem duplicar.

**Auto-criação de viagem_instancia**: pode ser que a cooperativa venda passagem para uma data futura cuja instância ainda não foi gerada pelo job. O webhook deve criar a instância on-the-fly se não existir, baseado no template correspondente.

**Overbooking permitido com prioridade do guichê**: regra de negócio explicitada no documento original. Se a viagem está em 40/40 vagas (vendidas pelo app) e o guichê notifica mais uma venda, a passagem do guichê **é registrada** com `overbooking=false`, e a próxima venda no app passa a ser registrada com `overbooking=true`. Em outras palavras: o guichê tem prioridade — quem comprou no app por último é quem fica sinalizado como overbooking.

**Notificação ao cobrador em campo**: o cobrador precisa saber rapidamente quando uma venda externa muda as vagas disponíveis. No MVP, polling local de 10s no app no `GET /api/v1/viagens/{id}` é suficiente. Push notification fica para Fase 2.

**Log obrigatório**: toda chamada (sucesso, erro, duplicada) é registrada em `webhook_log` com payload completo em `jsonb`. Isso permite ao proprietário ver, no painel web, o histórico de integrações e debugar problemas.

### Erros que valem prever desde já

| Erro | Resposta HTTP | Ação |
|---|---|---|
| Token inválido | 401 Unauthorized | Loga em webhook_log com erro de auth |
| Payload malformado | 400 Bad Request | Loga em webhook_log com erro de validação |
| `id_externo` já processado | 409 Conflict | Retorna o passagem_id existente |
| Viagem não existe e template não encontrado | 422 Unprocessable | Não cria nada, sinaliza erro de configuração |
| Falha de banco | 500 Internal | Loga, cooperativa retentará |

---

## Como esses fluxos se conectam ao DER

Olhando os três diagramas em conjunto, fica claro por que algumas decisões de modelagem foram tomadas:

A coluna `id_externo` na `passagem` com unique parcial existe **para o diagrama 3** (idempotência do webhook da cooperativa). Sem ela, retentativas duplicariam passagens.

A coluna `versao` em `viagem_instancia` (`@Version`) existe **para os diagramas 1 e 3** (lock otimista contra concorrência na atualização de vagas). Sem ela, dois UPDATEs simultâneos perderiam um decremento.

A coluna `ultimo_polling_em` em `pagamento_pix` existe **para o diagrama 2** (controlar a janela mínima entre tentativas de polling). Sem ela, o job consultaria EFÍ a cada execução para todos os Pix pendentes.

A tabela `webhook_log` separada de `webhook_config` existe **para o diagrama 3** (rastreabilidade de cada chamada recebida). Sem ela, debug de integração com cooperativa vira impossível.

A coluna `overbooking` na `passagem` existe para **sinalização visual no app e snapshot de fechamento**, conforme decidido nas regras de negócio.

---

## Recomendação de ordem de implementação

Não tente fazer os três fluxos ao mesmo tempo. Sugestão:

1. **Sprint 1**: Caminho feliz completo (diagrama 1) com webhook funcionando em ambiente de homologação do EFÍ. Sem polling ainda. Sem comprovante ainda.
2. **Sprint 2**: Adicionar polling de fallback (diagrama 2). Testar simulando webhook que não chega.
3. **Sprint 3**: Geração de comprovante PNG e share intent WhatsApp.
4. **Sprint 4** (já parte da Fase 1F): Webhook da cooperativa (diagrama 3) com autenticação por token e idempotência.

Cada sprint entrega valor sozinho e os diagramas servem como referência durante a codificação.
