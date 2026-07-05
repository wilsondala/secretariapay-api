Fase 24G — Segurança real do robô WhatsApp IMETRO

Objetivo:
Ajustar o robô do WhatsApp para a realidade do primeiro cliente: Instituto Superior Politécnico Metropolitano de Angola (IMETRO).

Regras aplicadas:
1. O sistema localiza estudante por:
   - Número de estudante/carteira
   - E-mail cadastrado
   - Telefone cadastrado
   - Código da cobrança
   - BI/documento, quando existir no cadastro

2. Regra de ouro de segurança:
   Mesmo que o aluno solicite informação por outro telefone, o sistema não expõe dados financeiros nesse telefone.
   Guias, recibos e situação financeira só são enviados para contactos oficiais cadastrados na universidade.

3. Canais oficiais:
   - WhatsApp cadastrado do estudante
   - E-mail cadastrado do estudante ou encarregado
   - SMS/mock para telefone cadastrado quando não houver WhatsApp

4. Remoções/ajustes:
   - Remove exemplos genéricos do robô
   - Remove confirmação mock de pagamento pelo WhatsApp no fluxo do cliente
   - Personaliza o texto para IMETRO e DCR
   - Desativa busca por nome completo para evitar vazamento de dados

5. Nome institucional correto:
   Instituto Superior Politécnico Metropolitano de Angola (IMETRO)

Arquivos ajustados:
- src/main/java/com/secretariapay/api/repository/academic/StudentRepository.java
- src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappAcademicSupportService.java
- src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappBrainService.java

Comando para atualizar aluno de teste do cliente:
update students
set phone = '+244925939243', whatsapp = '+244925939243', updated_at = now()
where student_number = '20230294';

Aplicação local:
cd C:\Users\dalaw\secretariapay-api
Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-24g-seguranca-whatsapp-imetro.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "feat: secure IMETRO WhatsApp student lookup"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
nohup docker compose up -d --build > /tmp/secretariapay-deploy.log 2>&1 &
tail -f /tmp/secretariapay-deploy.log
