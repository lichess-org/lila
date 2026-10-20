# OIDC/SSO: инструкция по деплою

Runbook для выката изменений из `.kilo/plans/oidc-sso-hardening-plan.md`.
Все ключи конфигурации — HOCON; переопределяются через `conf/application.conf`
или `-D...` при запуске.

## 1. Что должно быть готово на стороне IdP (passport)

OIDC-клиент:

| Параметр | Значение |
|---|---|
| Redirect URI | `https://<lila-base-url>/signin-oidc` |
| Post-logout redirect URI | `https://<lila-base-url>/` |
| Backchannel logout URL | `https://<lila-base-url>/internal/identity/backchannel-logout` |
| Scopes | `openid profile email` |
| PKCE | обязателен, `S256` |
| `token_endpoint_auth_method` | `client_secret_post` (или `client_secret_basic`) |
| `id_token_signing_alg_values_supported` | должен содержать настроенный `jws_alg` (например `RS256`) |

Claims в ID token / UserInfo:

- `sub` — стабильный идентификатор; **становится `UserId` в lila**;
- `preferred_username` — display-name (Unicode, пробелы/дефисы допустимы);
- `email`, `email_verified`;
- `sid` — идентификатор SSO-сессии (для backchannel logout);
- `auth_time` — опционально;
- `globally_enabled` — опционально, `false` означает глобальный бан.

## 2. Конфигурация lila

```hocon
oidc {
  issuer {
    base_url          = "https://passport.chess-online.com"
    client_id         = "<из IdP>"
    client_secret     = "<из IdP>"
    scopes            = "openid profile email"
    jws_alg           = "RS256"
    token_auth_method = "client_secret_post"
  }
}

auth {
  ssoOnly = false   # см. порядок выката; включается последним
}

identity {
  webhook {
    secret = "<случайный HMAC-секрет>"
  }
}
```

Правила:

- `oidc.issuer.client_id` и `client_secret` обязательны в `prod`: приложение не стартует.
- `identity.webhook.secret` пустой → endpoint `/internal/identity/events` отвечает `404`.
- `jws_alg` обязателен; `none` и `HS*` запрещены.
- `token_auth_method` допустим только `client_secret_post` или `client_secret_basic`.

## 3. MongoDB: индексы и миграции

Порядок важен: миграцию email-индекса выполнить **до** первого входа двух разных
`sub` с одинаковым email.

```bash
# 1) снять unique-индекс с email (обязательно)
mongosh lila bin/mongodb/migrations/2026-10-20-drop-unique-email-index.js

# 2) привести существующие email к нормализованному виду (идемпотентно)
mongosh lila bin/mongodb/migrations/2026-10-20-normalize-emails.js

# 3) создать остальные индексы
mongosh lila bin/mongodb/indexes.js
```

Появятся индексы:

- `identity_status` — `version`, `status`;
- `identity_event` — `subject + version`;
- `oidc_state` — TTL по `expiresAt` (10 минут);
- `security` — `sid` (sparse) и TTL по `expiresAt` (30 дней);
- `user4` — `username` (для поиска по display-name), email без `unique`.

TTL-индексы удаляют документы автоматически; это нормально.

## 4. Порядок выката

1. Бэкап MongoDB (`user4`, `security`, `identity_status`, `identity_event`).
2. Прогнать миграции из раздела 3.
3. Выкатить приложение с `auth.ssoOnly = false`.
4. Проверить OIDC-вход (см. чек-лист).
5. Включить `auth.ssoOnly = true` и перезапустить приложение.
6. Настроить в IdP backchannel logout и webhook событий.
7. Проверить бан/удаление/выход (см. чек-лист).

`auth.ssoOnly = true` закрывает локальные password/signup/reset/magic-link,
2FA, смену email/username/password и удаление аккаунта: все отвечают `404`.

## 5. Webhook событий идентичности

`POST https://<lila-base-url>/internal/identity/events`

Заголовок:

```
X-Identity-Signature: <hex(HMAC-SHA256(raw_body, identity.webhook.secret))>
```

Тело (JSON, UTF-8, до 4 KiB):

```json
{
  "eventId": "evt-0001",
  "issuer": "https://passport.chess-online.com",
  "sub": "1001",
  "status": "banned",
  "version": 3,
  "occurredAt": "2026-10-20T06:00:00Z",
  "reasonCode": "abuse"
}
```

- `status`: `active` | `banned` | `suspended` | `deleted`.
- `eventId` идемпотентен; `version` монотонен, старые события игнорируются.
- `banned`/`suspended` → закрываются HTTP-сессии, appeal-сессии, OAuth-токены,
  push-подписки и активные WebSocket.
- `deleted` → то же + анонимизация username/email/profile (`_id` и игры сохраняются).
- `active` после `deleted` восстанавливает только локальный tombstone.

Backchannel logout:

`POST https://<lila-base-url>/internal/identity/backchannel-logout`
(`application/x-www-form-urlencoded`, поле `logout_token`).
Токен проверяется по JWKS, `iss`, `aud` и событию
`http://schemas.openid.net/event/backchannel-logout`; закрываются сессии с
указанным `sid` (или все сессии при наличии только `sub`).

## 6. Проверка после выката

- [ ] `https://<base>/@/<sub>` открывает профиль; `https://<base>/@/<display-name>` → `404`.
- [ ] Вход через IdP создаёт сессию; в `db.security` у сессии есть `idToken`, `sid`, `expiresAt`.
- [ ] Повторный callback с тем же `state` → отказ (state одноразовый).
- [ ] `state` без cookie `oidc_state` → отказ.
- [ ] Logout уводит на `end_session_endpoint` IdP, затем возвращает на `/`.
- [ ] Backchannel logout закрывает сессию и WS.
- [ ] Событие `banned` закрывает сессии/OAuth/push/WS.
- [ ] Событие `deleted` очищает username/email/profile.
- [ ] `auth.ssoOnly=true`: `/login` (POST), `/password/reset`, `/account/passwd`, `/account/email`, `/account/twofactor` → `404`.

Пример подписи для ручной проверки:

```bash
SECRET='...'
BODY='{"eventId":"evt-1","issuer":"https://passport.chess-online.com","sub":"1001","status":"banned","version":1,"occurredAt":"2026-10-20T06:00:00Z"}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SECRET" -hex | awk '{print $2}')
curl -sS -X POST https://<base>/internal/identity/events \
  -H "Content-Type: application/json" \
  -H "X-Identity-Signature: $SIG" \
  --data-binary "$BODY"
```

## 7. Откат

1. `auth.ssoOnly = false`, перезапуск.
2. Откатить приложение на предыдущий релиз.
3. Индексы и `identity_*` коллекции можно оставить; они не влияют на старый код.
4. Unique-индекс email возвращать только если в `user4` нет дублей:
   `db.user4.aggregate([{$group:{_id:"$email",n:{$sum:1}}},{$match:{n:{$gt:1}}}])`.

## 8. Ограничения текущего этапа

- `line-ws` и `arena` (отдельные репозитории) ещё не проверяют `identity_status`.
- Refresh tokens не реализованы (4.4 — опционально).
- Интеграционные тесты с mock IdP/БД не добавлены: в репозитории нет тестового
  Mongo/Play-контура (Phase 8).
- Локальные формы, где пользователь вводит имя вручную (`msg` compat,
  `GameSearchForm`), ожидают ID; им нужен autocomplete (Phase 3.3/8.4).
