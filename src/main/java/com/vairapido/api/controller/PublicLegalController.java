package com.vairapido.api.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/legal")
public class PublicLegalController {

    @GetMapping(value = "/privacy-policy", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> privacyPolicy() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("""
                        <!doctype html>
                        <html lang="pt-BR">
                        <head>
                            <meta charset="UTF-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                            <title>Política de Privacidade - VaiRápido</title>
                            <style>
                                body { font-family: Arial, sans-serif; margin: 0; background: #f8fafc; color: #0f172a; line-height: 1.6; }
                                header { background: #071B33; color: white; padding: 32px 20px; border-bottom: 8px solid #FFC107; }
                                main { max-width: 920px; margin: 0 auto; padding: 28px 20px 48px; }
                                h1 { margin: 0; font-size: 36px; }
                                h2 { margin-top: 28px; color: #071B33; }
                                .card { background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 28px; box-shadow: 0 12px 30px rgba(15,23,42,.08); }
                                .muted { color: #64748b; }
                            </style>
                        </head>
                        <body>
                            <header>
                                <h1>VaiRápido</h1>
                                <p>Política de Privacidade</p>
                            </header>
                            <main>
                                <section class="card">
                                    <p class="muted">Última atualização: 20/06/2026</p>

                                    <h2>1. Sobre o VaiRápido</h2>
                                    <p>
                                        O VaiRápido é uma plataforma digital em fase de testes para consulta, reserva,
                                        pagamento simulado e emissão de bilhetes digitais de passagem pelo WhatsApp e pela API.
                                    </p>

                                    <h2>2. Dados que podemos coletar</h2>
                                    <p>
                                        Durante os testes, podemos tratar dados como nome, telefone/WhatsApp, documento do passageiro,
                                        origem, destino, data da viagem, reserva, pagamento simulado, bilhete e registros técnicos de uso.
                                    </p>

                                    <h2>3. Finalidade do uso dos dados</h2>
                                    <p>
                                        Os dados são usados para permitir o fluxo de compra de passagem, criação de reserva,
                                        confirmação de pagamento simulado, emissão de bilhete, envio de PDF pelo WhatsApp,
                                        validação por QR Code e segurança operacional.
                                    </p>

                                    <h2>4. WhatsApp e Meta</h2>
                                    <p>
                                        O VaiRápido usa a WhatsApp Business Platform para receber mensagens, responder dúvidas,
                                        enviar confirmações, bilhetes e documentos relacionados ao atendimento solicitado pelo usuário.
                                    </p>

                                    <h2>5. Compartilhamento de dados</h2>
                                    <p>
                                        Os dados podem ser compartilhados apenas com serviços necessários para funcionamento da plataforma,
                                        como provedores de infraestrutura, WhatsApp Business Platform/Meta e sistemas internos autorizados.
                                    </p>

                                    <h2>6. Segurança</h2>
                                    <p>
                                        Aplicamos controles técnicos para proteger as informações, incluindo autenticação,
                                        controle de permissões, HTTPS, registros de auditoria e segregação de ambientes.
                                    </p>

                                    <h2>7. Retenção e exclusão</h2>
                                    <p>
                                        Dados de teste podem ser removidos mediante solicitação. Para pedir exclusão de dados,
                                        acesse a página de instruções de exclusão de dados.
                                    </p>

                                    <h2>8. Contato</h2>
                                    <p>
                                        Para dúvidas sobre privacidade ou solicitação de exclusão de dados, entre em contato:
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
                        <html lang="pt-BR">
                        <head>
                            <meta charset="UTF-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                            <title>Termos de Serviço - VaiRápido</title>
                            <style>
                                body { font-family: Arial, sans-serif; margin: 0; background: #f8fafc; color: #0f172a; line-height: 1.6; }
                                header { background: #071B33; color: white; padding: 32px 20px; border-bottom: 8px solid #FFC107; }
                                main { max-width: 920px; margin: 0 auto; padding: 28px 20px 48px; }
                                h1 { margin: 0; font-size: 36px; }
                                h2 { margin-top: 28px; color: #071B33; }
                                .card { background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 28px; box-shadow: 0 12px 30px rgba(15,23,42,.08); }
                                .muted { color: #64748b; }
                            </style>
                        </head>
                        <body>
                            <header>
                                <h1>VaiRápido</h1>
                                <p>Termos de Serviço</p>
                            </header>
                            <main>
                                <section class="card">
                                    <p class="muted">Última atualização: 20/06/2026</p>

                                    <h2>1. Sobre o serviço</h2>
                                    <p>
                                        O VaiRápido é uma plataforma digital em fase de testes para consulta de viagens,
                                        reserva de passagens, confirmação de pagamento simulado, emissão de bilhete digital
                                        e validação por QR Code.
                                    </p>

                                    <h2>2. Uso permitido</h2>
                                    <p>
                                        O usuário deve utilizar o serviço apenas para fins legítimos de teste, consulta,
                                        reserva e validação de bilhetes. É proibido tentar fraudar reservas, reutilizar bilhetes,
                                        acessar áreas restritas sem autorização ou interferir no funcionamento da plataforma.
                                    </p>

                                    <h2>3. Ambiente de teste</h2>
                                    <p>
                                        O VaiRápido encontra-se em ambiente de validação técnica. Algumas funcionalidades,
                                        valores, rotas, empresas de transporte, métodos de pagamento e mensagens podem ser
                                        simulados e alterados antes da versão final de produção.
                                    </p>

                                    <h2>4. Pagamentos</h2>
                                    <p>
                                        No ambiente atual, os pagamentos podem ser simulados para validação do fluxo.
                                        Integrações reais de pagamento serão implementadas conforme a necessidade do projeto
                                        e das empresas parceiras.
                                    </p>

                                    <h2>5. Bilhete digital</h2>
                                    <p>
                                        O bilhete digital emitido pelo VaiRápido pode conter código de reserva, código de bilhete,
                                        dados do passageiro, trecho, horário, poltrona, QR Code e link de validação pública.
                                        O uso no embarque depende das regras operacionais da empresa de transporte.
                                    </p>

                                    <h2>6. Disponibilidade</h2>
                                    <p>
                                        Durante a fase de testes, o serviço pode sofrer interrupções, ajustes técnicos,
                                        alterações de fluxo e atualizações sem aviso prévio.
                                    </p>

                                    <h2>7. Responsabilidades</h2>
                                    <p>
                                        O usuário é responsável por fornecer informações corretas durante os testes.
                                        A plataforma poderá registrar eventos técnicos, auditorias e validações para segurança
                                        e melhoria do serviço.
                                    </p>

                                    <h2>8. Privacidade</h2>
                                    <p>
                                        O tratamento de dados segue a Política de Privacidade do VaiRápido, disponível
                                        publicamente na página de privacidade.
                                    </p>

                                    <h2>9. Contato</h2>
                                    <p>
                                        Para dúvidas sobre estes termos, entre em contato:
                                        geral@triacompany.com
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
                        <html lang="pt-BR">
                        <head>
                            <meta charset="UTF-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                            <title>Exclusão de Dados - VaiRápido</title>
                            <style>
                                body { font-family: Arial, sans-serif; margin: 0; background: #f8fafc; color: #0f172a; line-height: 1.6; }
                                header { background: #071B33; color: white; padding: 32px 20px; border-bottom: 8px solid #FFC107; }
                                main { max-width: 920px; margin: 0 auto; padding: 28px 20px 48px; }
                                h1 { margin: 0; font-size: 36px; }
                                h2 { margin-top: 28px; color: #071B33; }
                                .card { background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 28px; box-shadow: 0 12px 30px rgba(15,23,42,.08); }
                            </style>
                        </head>
                        <body>
                            <header>
                                <h1>VaiRápido</h1>
                                <p>Instruções para Exclusão de Dados</p>
                            </header>
                            <main>
                                <section class="card">
                                    <h2>Como solicitar a exclusão</h2>
                                    <p>
                                        Para solicitar a exclusão dos seus dados de teste no VaiRápido,
                                        envie um e-mail para geral@triacompany.com com o assunto:
                                        "Exclusão de dados - VaiRápido".
                                    </p>

                                    <h2>Informações necessárias</h2>
                                    <p>
                                        Informe o telefone/WhatsApp usado no teste e, se possível,
                                        o código da reserva ou do bilhete.
                                    </p>

                                    <h2>Prazo de atendimento</h2>
                                    <p>
                                        Após confirmação da solicitação, os dados serão analisados e removidos
                                        dos ambientes de teste quando aplicável.
                                    </p>
                                </section>
                            </main>
                        </body>
                        </html>
                        """);
    }
}