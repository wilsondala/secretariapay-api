# SecretáriaPay Multiuniversidade

O SecretáriaPay deve funcionar como plataforma SaaS da TRIA Company para atender várias universidades, institutos, colégios e centros de formação.

## Regra principal

Todo dado institucional deve estar ligado, direta ou indiretamente, a uma `Institution`.

Exemplo:

```text
Institution
 ├── Courses
 ├── AcademicClasses
 ├── Students
 ├── Charges
 ├── PaymentProofs
 ├── Receipts
 ├── AcademicBlocks
 └── Users
```

## Papéis principais

```text
ADMIN_GLOBAL
ADMIN_INSTITUTION
DIRECAO
FINANCEIRO
TESOURARIA
SECRETARIA
OPERADOR_ATENDIMENTO
```

## Primeiro cliente

```text
Universidade Metropolitana de Angola / IMETRO
```

O IMETRO é o primeiro piloto, mas a plataforma deve estar pronta para receber outras instituições sem misturar dados.

## Estratégia

A TRIA Company administra o produto como `ADMIN_GLOBAL`.
Cada universidade possui usuários próprios com acesso apenas aos seus dados.
