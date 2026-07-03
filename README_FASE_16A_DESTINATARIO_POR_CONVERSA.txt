Fase 16A — Corrigir destinatário da guia e do recibo por conversa

Problema corrigido:
- Quando o aluno testa de outro número, a guia/recibo não deve ir sempre para o telefone cadastrado antigo.
- O atendimento deve responder e enviar documentos para o número que iniciou a conversa, depois que o estudante foi identificado e confirmado.

O que muda:
1. Guia de pagamento: enviada para o WhatsApp que está conversando agora.
2. Recibo automático mock: enviado para o mesmo WhatsApp que confirmou o pagamento.
3. O telefone ativo da conversa fica guardado em metadata como conversationRecipientPhone.
4. O telefone cadastrado do aluno continua existindo, mas não força o envio durante atendimento iniciado por outro número.

Arquivos alterados:
- src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappFinancialConversationService.java
- src/main/java/com/secretariapay/api/service/financial/SecretariaPayMockAutomaticPaymentService.java

Teste WhatsApp:
1. Em outro número, envie: menu
2. Envie: 2
3. Envie: IMETRO-2026-TESTE-002
4. Confirme: 1
5. O PDF da guia deve chegar nesse mesmo número.
6. Responda: recebi
7. Responda: paguei
8. O PDF do recibo deve chegar nesse mesmo número.
9. Responda: obrigado

Confirmação no banco:
select type, charge_code, receipt_code, recipient_phone, status, provider_message_id, sent_at
from secretariapay_messages
where charge_code = 'COLE_CHARGE_CODE'
order by created_at desc;

O recipient_phone do PAYMENT_GUIDE/RECEIPT_ISSUED deve ser o número que conversou com o robô.
