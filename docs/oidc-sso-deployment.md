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
  # Локальные password/signup/reset/magic-link/credential-роуты удалены из приложения.
  # Флаг оставлен для совместимости: при true вход не требует локального email-confirm.
  ssoOnly = true
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

# 3) пересчитать len у username (нужен для поиска/сортировки)
mongosh lila bin/mongodb/migrations/2026-10-20-recompute-username-len.js

# 4) проставить expiresAt старым сессиям и удалить сессии старше 30 дней
#    ВНИМАНИЕ: разлогинивает сессии старше 30 дней, выполнять в окно обслуживания
mongosh lila bin/mongodb/migrations/2026-10-20-security-session-ttl.js

# 5) создать остальные индексы
mongosh lila bin/mongodb/indexes.js
```

Появятся индексы:

- `identity_status` — `version`, `status`;
- `identity_event` — `subject + version`;
- `oidc_state` — TTL по `expiresAt` (10 минут);
- `security` — `sid` (sparse) и TTL по `expiresAt` (30 дней);
- `user4` — `username` (для поиска по display-name), email без `unique`.

TTL-индексы удаляют документы автоматически; это нормально.

### Legacy ID и alias-таблица

В прод-дампах встречаются аккаунты, чей `_id` — старый username-подобный id,
а не SSO `sub` (по аудиту: 7 из 31 888). Чтобы при первом входе не создать
дубликат аккаунта, заполните `identity_alias` соответствием `sub → legacy id`
(данные берутся из выгрузки IdP: `sub` и старый идентификатор/username):

```js
db.identity_alias.insertOne({ _id: "<sub>", userId: "<legacy_lila_id>" })
```

`UserProvisioning` сначала ищет `_id = sub`, затем alias. Заполнять до включения
SSO-входа для этих пользователей.

### Аудит перед выкатом

Read-only аудит по копии дампа:

```bash
mongosh lichess bin/mongodb/audit-oidc-migration.js
```

Скрипт не печатает email/полные username — только агрегаты и маскированные
примеры. На прод-дампах ожидаемы: неканонические id, устаревший `len`,
дубли email, `security` без `expiresAt`.

## 4. Порядок выката

1. Бэкап MongoDB (`user4`, `security`, `identity_status`, `identity_event`).
2. Прогнать миграции из раздела 3.
3. Выкатить приложение.
4. Проверить OIDC-вход (см. чек-лист).
5. Настроить в IdP backchannel logout и webhook событий.
6. Проверить бан/удаление/выход (см. чек-лист).

Локальные password/signup/reset/magic-link, 2FA, смена email/username/password
и удаление аккаунта удалены из `conf/routes`: эти URL отвечают `404` независимо
от флага. `auth.ssoOnly` оставлен для совместимости и пропуска email-confirm.

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
- `issuer` должен точно совпадать с `oidc.issuer.base_url`, иначе событие отклоняется.
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
- [ ] `state` без cookie `oidc_state` → отказ (cookie `HttpOnly`, `SameSite=Lax`, TTL 10 мин).
- [ ] Logout уводит на `end_session_endpoint` IdP, затем возвращает на `/`.
- [ ] Backchannel logout закрывает сессию и WS.
- [ ] Событие `banned` закрывает сессии/OAuth/push/WS.
- [ ] Событие `deleted` очищает username/email/profile.
- [ ] `/login` (POST), `/signup` (POST), `/password/reset`, `/account/passwd`, `/account/email`, `/account/twofactor` → `404` (маршруты удалены).

### Smoke-тест

Для запущенного стека (в т.ч. `lila-docker`) есть готовый набор проверок:

```bash
BASE_URL=http://localhost:8080 bin/tests/oidc-smoke
# с проверкой webhook:
BASE_URL=http://localhost:8080 WEBHOOK_SECRET=<secret> bin/tests/oidc-smoke
```

Проверяет: редиректы SSO, отсутствие локальных маршрутов, ID-only профили,
PKCE/S256, scopes, state-cookie, одноразовый state, backchannel 400,
HMAC-подпись, идемпотентность и отказ при чужом `issuer`.

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
