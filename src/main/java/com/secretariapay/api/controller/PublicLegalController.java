package com.secretariapay.api.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/legal")
public class PublicLegalController {

    @GetMapping(value = "/privacy-policy", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> privacyPolicy() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("""
                        <!doctype html>
                        <html lang="pt-AO">
                        <head>
                            <meta charset="UTF-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                            <title>Política de Privacidade - SecretáriaPay</title>
                            <style>
                                body { font-family: Arial, sans-serif; margin: 0; background: #f8fafc; color: #0f172a; line-height: 1.6; }
                                header { background: #0B3B82; color: white; padding: 32px 20px; border-bottom: 8px solid #D4AF37; }
                                main { max-width: 920px; margin: 0 auto; padding: 28px 20px 48px; }
                                h1 { margin: 0; font-size: 36px; }
                                h2 { margin-top: 28px; color: #0B3B82; }
                                .card { background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 28px; box-shadow: 0 12px 30px rgba(15,23,42,.08); }
                                .muted { color: #64748b; }
                                a { color: #0B3B82; }
                            </style>
                        </head>
                        <body>
                            <header>
                                <h1>SecretáriaPay</h1>
                                <p>Política de Privacidade</p>
                            </header>
                            <main>
                                <section class="card">
                                    <p class="muted">Última atualização: 02/07/2026</p>

                                    <h2>1. Sobre o SecretáriaPay</h2>
                                    <p>
                                        O SecretáriaPay Académico é uma plataforma institucional de automação de propinas,
                                        cobranças, comprovativos, recibos digitais e atendimento académico via WhatsApp,
                                        desenvolvida pela TRIA Company para apoiar instituições de ensino em Angola.
                                    </p>

                                    <h2>2. Dados que podemos tratar</h2>
                                    <p>
                                        A plataforma pode tratar dados como nome do estudante, número académico,
                                        telefone/WhatsApp, documento de identificação, curso, turma, turno, ano académico,
                                        cobranças, vencimentos, comprovativos de pagamento, recibos digitais, histórico de
                                        mensagens, estado financeiro e registos técnicos de auditoria.
                                    </p>

                                    <h2>3. Finalidade do uso dos dados</h2>
                                    <p>
                                        Os dados são utilizados para organizar cobranças académicas, enviar avisos de vencimento
                                        e atraso, receber e validar comprovativos, emitir recibos digitais, apresentar relatórios,
                                        apoiar a secretaria e a tesouraria e, quando autorizado pela instituição, atualizar a
                                        situação académica do estudante conforme regras internas.
                                    </p>

                                    <h2>4. WhatsApp e comunicação institucional</h2>
                                    <p>
                                        O SecretáriaPay pode usar a WhatsApp Business Platform ou provedor homologado para
                                        receber mensagens, enviar avisos, confirmar recebimento de comprovativos, informar
                                        aprovação de pagamentos, disponibilizar recibos digitais e orientar o estudante em
                                        fluxos académicos autorizados pela instituição.
                                    </p>

                                    <h2>5. Compartilhamento de dados</h2>
                                    <p>
                                        Os dados podem ser compartilhados apenas com a instituição contratante, provedores de
                                        infraestrutura, serviços de mensageria, meios de pagamento e sistemas académicos ou
                                        financeiros autorizados. Não vendemos dados de estudantes.
                                    </p>

                                    <h2>6. Segurança</h2>
                                    <p>
                                        Aplicamos controles técnicos para proteger as informações, incluindo HTTPS,
                                        autenticação, perfis de acesso, auditoria, segregação de responsabilidades,
                                        registos de operação e boas práticas de infraestrutura.
                                    </p>

                                    <h2>7. Retenção e exclusão</h2>
                                    <p>
                                        A retenção dos dados depende das regras da instituição contratante, obrigações legais,
                                        necessidades de auditoria financeira e políticas internas. Solicitações de acesso,
                                        correção ou exclusão devem ser encaminhadas pelos canais oficiais indicados pela
                                        instituição ou pela TRIA Company.
                                    </p>

                                    <h2>8. Contato</h2>
                                    <p>
                                        Para dúvidas sobre privacidade, proteção de dados ou solicitação de exclusão, entre em contato:
                                        geral@triacompany.com
                                    </p>
                                </section>
                            </main>
                        </body>
                        </html>
                        """);
    }

    @GetMapping(value = "/terms-of-service", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> termsOfService() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("""
                        <!doctype html>
                        <html lang="pt-AO">
                        <head>
                            <meta charset="UTF-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                            <title>Termos de Serviço - SecretáriaPay</title>
                            <style>
                                body { font-family: Arial, sans-serif; margin: 0; background: #f8fafc; color: #0f172a; line-height: 1.6; }
                                header { background: #0B3B82; color: white; padding: 32px 20px; border-bottom: 8px solid #D4AF37; }
                                main { max-width: 920px; margin: 0 auto; padding: 28px 20px 48px; }
                                h1 { margin: 0; font-size: 36px; }
                                h2 { margin-top: 28px; color: #0B3B82; }
                                .card { background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 28px; box-shadow: 0 12px 30px rgba(15,23,42,.08); }
                                .muted { color: #64748b; }
                            </style>
                        </head>
                        <body>
                            <header>
                                <h1>SecretáriaPay</h1>
                                <p>Termos de Serviço</p>
                            </header>
                            <main>
                                <section class="card">
                                    <p class="muted">Última atualização: 02/07/2026</p>

                                    <h2>1. Sobre o serviço</h2>
                                    <p>
                                        O SecretáriaPay Académico é uma plataforma institucional para automação de cobranças
                                        académicas, gestão de propinas, comprovativos, recibos digitais, histórico de atendimento
                                        e comunicação via WhatsApp entre instituição e estudantes.
                                    </p>

                                    <h2>2. Uso permitido</h2>
                                    <p>
                                        O serviço deve ser utilizado por estudantes, colaboradores, tesouraria, secretaria,
                                        direção e administradores autorizados para fins legítimos de atendimento académico,
                                        cobrança, validação financeira, emissão de recibos e consulta de situação institucional.
                                    </p>

                                    <h2>3. Responsabilidade da instituição</h2>
                                    <p>
                                        A instituição contratante é responsável por validar regras de cobrança, valores,
                                        multas, juros, prazos, políticas de bloqueio/desbloqueio, permissões de acesso e
                                        conteúdo das comunicações enviadas aos estudantes.
                                    </p>

                                    <h2>4. Pagamentos e comprovativos</h2>
                                    <p>
                                        O SecretáriaPay pode registar cobranças, receber comprovativos, apoiar validação pela
                                        tesouraria e emitir recibos digitais após aprovação. Integrações bancárias ou com
                                        carteiras móveis dependem de disponibilidade técnica, autorização e contrato específico.
                                    </p>

                                    <h2>5. Recibos digitais e validação pública</h2>
                                    <p>
                                        Recibos digitais podem conter código único, QR Code, dados do estudante, cobrança,
                                        estado do pagamento e link público de validação. A validade institucional do recibo
                                        depende da aprovação financeira registada pela instituição.
                                    </p>

                                    <h2>6. Bloqueio e desbloqueio académico</h2>
                                    <p>
                                        Qualquer bloqueio ou desbloqueio de serviços académicos deve seguir política formal
                                        definida e autorizada pela instituição. A plataforma apenas executa ou apoia regras
                                        configuradas por perfis autorizados.
                                    </p>

                                    <h2>7. Disponibilidade e evolução</h2>
                                    <p>
                                        Durante fases piloto, homologação ou implantação, funcionalidades podem sofrer ajustes,
                                        melhorias, interrupções técnicas ou mudanças de fluxo para adequação às necessidades
                                        da instituição e às integrações disponíveis.
                                    </p>

                                    <h2>8. Privacidade</h2>
                                    <p>
                                        O tratamento de dados segue a Política de Privacidade do SecretáriaPay e as regras
                                        acordadas com a instituição contratante.
                                    </p>

                                    <h2>9. Contato</h2>
                                    <p>
                                        Para dúvidas sobre estes termos, entre em contato: geral@triacompany.com
                                    </p>
                                </section>
                            </main>
                        </body>
                        </html>
                        """);
    }

    @GetMapping(value = "/data-deletion", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> dataDeletion() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("""
                        <!doctype html>
                        <html lang="pt-AO">
                        <head>
                            <meta charset="UTF-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                            <title>Exclusão de Dados - SecretáriaPay</title>
                            <style>
                                body { font-family: Arial, sans-serif; margin: 0; background: #f8fafc; color: #0f172a; line-height: 1.6; }
                                header { background: #0B3B82; color: white; padding: 32px 20px; border-bottom: 8px solid #D4AF37; }
                                main { max-width: 920px; margin: 0 auto; padding: 28px 20px 48px; }
                                h1 { margin: 0; font-size: 36px; }
                                h2 { margin-top: 28px; color: #0B3B82; }
                                .card { background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 28px; box-shadow: 0 12px 30px rgba(15,23,42,.08); }
                            </style>
                        </head>
                        <body>
                            <header>
                                <h1>SecretáriaPay</h1>
                                <p>Instruções para Exclusão de Dados</p>
                            </header>
                            <main>
                                <section class="card">
                                    <h2>Como solicitar a exclusão</h2>
                                    <p>
                                        Para solicitar análise de exclusão, correção ou acesso aos seus dados tratados pelo
                                        SecretáriaPay, envie um e-mail para geral@triacompany.com com o assunto:
                                        "Exclusão de dados - SecretáriaPay".
                                    </p>

                                    <h2>Informações necessárias</h2>
                                    <p>
                                        Informe o nome completo, telefone/WhatsApp usado no atendimento, instituição,
                                        número de estudante, documento de identificação e, se possível, código de cobrança,
                                        comprovativo ou recibo relacionado.
                                    </p>

                                    <h2>Análise da solicitação</h2>
                                    <p>
                                        A solicitação será analisada considerando obrigações legais, auditoria financeira,
                                        políticas internas da instituição contratante e necessidade de preservação de registos
                                        para comprovação de pagamentos e atendimento académico.
                                    </p>

                                    <h2>Prazo de atendimento</h2>
                                    <p>
                                        Após confirmação da identidade e validação com a instituição responsável, a solicitação
                                        será tratada conforme a política aplicável e a natureza dos dados envolvidos.
                                    </p>
                                </section>
                            </main>
                        </body>
                        </html>
                        """);
    }
}
