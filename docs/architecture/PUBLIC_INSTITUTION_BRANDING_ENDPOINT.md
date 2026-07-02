# Endpoint público de branding institucional

## Endpoint

```http
GET /api/v1/public/branding/institutions/{slug}
```

Exemplo:

```http
GET /api/v1/public/branding/institutions/imetro
```

## Objetivo

Disponibilizar os dados públicos da instituição cliente para o frontend, páginas públicas, cabeçalhos, recibos e telas personalizadas por universidade.

## Segurança

A rota fica abaixo de `/api/v1/public/**`, portanto é pública e não exige JWT. Nenhum token, senha, chave da Meta ou dado sensível é retornado.

## Campos retornados

- institutionName
- legalName
- slug
- slogan
- address
- country
- currency
- timezone
- officialWhatsapp
- supportEmail
- academicPortalBaseUrl
- platform
- platformLogoUrl
- primaryColor
- secondaryColor
- accentColor
- active

## Observação

Nesta primeira versão, slogan e cores são resolvidos por slug. Em fase posterior, estes campos podem migrar para uma tabela própria de branding institucional.
