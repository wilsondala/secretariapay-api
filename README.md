# SecretáriaPay Académico API

Backend institucional para gestão automatizada de propinas, cobranças, comprovativos, recibos digitais, admissões, matrículas, serviços académicos e atendimento via WhatsApp.

## Escopo

O SecretáriaPay Académico centraliza:

- cadastro institucional, cursos, turmas e estudantes;
- inscrições, admissões, matrículas e rematrículas;
- geração e acompanhamento de cobranças académicas;
- guias de pagamento, comprovativos e recibos digitais;
- regras financeiras, multas, bloqueios e regularização académica;
- pedidos de serviços e emissão de documentos académicos;
- notificações e atendimento financeiro pelo WhatsApp;
- painéis de DCR, Secretaria, Direção, TIC e Auditoria.

## Stack

- Java 21;
- Spring Boot;
- Spring Security e JWT;
- PostgreSQL 16;
- Flyway;
- Docker e Docker Compose;
- NGINX e SSL em produção.

## Execução local

1. Crie o arquivo `.env.production` a partir do `.env.example`.
2. Configure as credenciais locais.
3. Suba os serviços:

```bash
docker compose up -d --build
```

4. Valide a API:

```bash
curl http://localhost:8080/actuator/health
```

## Testes

```bash
mvn test
```

## Segurança operacional

- não versionar segredos nem credenciais reais;
- manter utilizadores de teste desativados em produção;
- ativar integrações externas somente após homologação;
- executar backup do PostgreSQL antes de aplicar migrations em produção;
- validar Flyway, health check e logs após cada atualização.

## Estrutura atual

O código ativo é exclusivamente académico-financeiro. As antigas tabelas técnicas de transporte e bilhética permanecem apenas nas migrations históricas já aplicadas, cujos checksums não podem ser alterados. A migration de desacoplamento remove essas estruturas do banco em execução sem reescrever o histórico do Flyway.

## Identidade

Projeto independente identificado publicamente como **SecretáriaPay Académico**, com configuração institucional para o IMETRO.
