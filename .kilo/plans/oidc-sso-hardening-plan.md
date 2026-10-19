# План работ: SSO-only hardening (OIDC + модель идентичности)

- **Дата:** 2026-10-19
- **Статус:** draft, решения D1–D12 подтверждены; ревизия по результатам сверки с кодом — 2026-10-19 (добавлены 1.0, 1.7, 3.8; расширены 1.1/2.1/2.2/2.5/2.5a/3.5/4.1/4.6/8.1–8.4/Phase 9)
- **Репозиторий:** `D:\Workspace\Projects\lila\line-repos\line`
- **Область:** `modules/oidc`, `app/controllers/Auth.scala`, `app/controllers/Account.scala`, `modules/core` (`userId.scala`), `modules/ui` (`router/LilaRouter.scala`), `modules/user`, `modules/security`, `modules/oauth`, `modules/team`, `modules/msg`, `modules/tournament`, `conf/routes`, `conf/base.conf`, `bin/mongodb/indexes.js`
- **Связанные репозитории:** `arena/src` (клиент: sessionId/socket/http) и `line-ws` (WS: TTL/`enabled`/`identity_status`/disconnect). Изменения там — отдельные PR, но входят в DoD этого плана (3.8, 4.6).
- **Вне области (отдельная задача):** signature-flow для YandexGame / VkGame / FridayGame. План должен оставить для него точку расширения, но не реализует его.

---

## 0. Контекст

Форк lila предназначен только для работы с внешним SSO. Прямой локальный вход в lila запрещён. Текущее состояние — dev/переход, запрет ещё нигде не enforced.

Факты, на которых построен план:

1. IdP: `https://passport.chess-online.com`, OIDC discovery доступен.
2. `sub` — целое число, приведённое к строке. Используется как `_id` пользователя lila.
3. `preferred_username` — отдельный username, не равный `sub`. Это ключевое отличие от оригинальной lila, где `_id == lowercased(username)`.
4. База SSO старая (около 20 лет, ~350 000 пользователей). База lila изначально пустая, пользователи создаются при первом входе. Явной миграции нет и не планируется.
5. Баны должны быть двух уровней:
   - глобальный — на всю экосистему, источник истины IdP;
   - локальный — только lila (игровые ограничения/закрытие аккаунта).
6. Email-флоу (password reset, magic link) должны быть выключены.
7. IdP поддерживает `end_session_endpoint`, frontchannel и backchannel logout.
8. IdP discovery: `scopes_supported = openid, profile, email, phone, offline_access`; `address` и `profile_ex` не поддерживаются.
9. IdP поддерживает `response_modes_supported = query, fragment, form_post`; текущий рабочий режим — query.
10. Мобильный/игровой клиент будет использовать отдельный flow через IdP; это следующая задача.

---

## 1. Принятые допущения

Если решение по любому пункту отличается, меняется только соответствующий шаг; архитектура плана остаётся.

- **A1.** `sub` стабилен и никогда не переиспользуется в SSO — **подтверждено** (D3).
- **A2.** `preferred_username` — UTF-8 username: буквы/цифры (включая кириллицу), `_`, `-`, пробел; длина 2–30. Может меняться, уникальность **не гарантируется**. Используется только для отображения/поиска, никогда как ключ идентичности или URL-параметр (D2/D11 подтверждены).
- **A3.** Глобальный бан: IdP — source of truth, отдаёт signed claim `globally_enabled` и signed-события изменения статуса. Lila хранит проекцию `identity_status`, не пишет в БД lila напрямую со стороны IdP; при запрете закрывает сессии.
- **A4.** Локальный бан/закрытие lila хранится в `user4.enabled` и `user4.foreverClosed`.
- **A5.** Локальный вход (пароль, локальный signup, password reset, magic link, login token) должен быть недоступен на уровне приложения.
- **A6.** `response_mode=query` — единственный поддерживаемый режим (D6). `form_post` не рассматриваем; `POST /signin-oidc` удаляется.
- **A7.** Локальная 2FA полностью отключена (D7); MFA — ответственность IdP. Пользователь не может включить локальный TOTP, который OIDC всё равно не проверяет.
- **A8.** lila хранит только канонический `UserId = sub`. Платформенные ID (Yandex/VK/Friday) не попадают в `_id`.
- **A9.** Email — необязательные контактные данные; дубли разрешены, уникальность не гарантируется и не проверяется в lila. Email не используется для identity/входа (D5).
- **A10.** Все идентификационные ссылки и записи внутри lila используют `UserId`; `UserName` — только отображение.

---

## 2. Блокирующие решения

| ID | Решение | Предлагаемый default | Влияние |
|---|---|---|---|
| D1 | Точка запрета локального входа | **✅ Вариант A:** app-level guard + удаление роутов; nginx — дополнительный слой; локальные endpoint'ы → 404 | Phase 1 |
| D2 | Формат `preferred_username`: алфавитно-цифровой или числовой; может ли совпасть с чужим `_id` | **✅ Подтверждено (см. D11):** UTF-8 username `[\p{L}\p{N}_\- ]{2,30}`, уникальность не гарантируется, только отображение; identity — всегда `UserId` | Phase 3 |
| D3 | Гарантия, что SSO никогда не переиспользует `sub` | **✅ Подтверждено:** SSO гарантирует неизменность `sub`; `_id = sub` остаётся, добавляем мониторинг аномалий | Phase 2/3 |
| D4 | Форма глобального бана | **✅ Согласовано:** IdP — source of truth; signed claim `globally_enabled` + signed events; lila хранит проекцию `identity_status`; прямой записи в БД lila нет; backchannel logout | Phase 2/4 |
| D5 | Политика при duplicate email | **✅ Подтверждено:** дубли email разрешены; email не используется для идентификации/входа; unique index на `email` убрать; уникальность — зона IdP; коммуникации — в Discourse | Phase 2/7 |
| D6 | `response_mode` | **✅ Подтверждено:** только `query`; только GET `/signin-oidc`; POST-роут удалить; `form_post` не рассматриваем | Phase 5 |
| D7 | Локальная 2FA | **✅ Подтверждено:** локальная 2FA полностью отключается; настройки/роуты скрываются; `totpSecret` не влияет на вход; MFA — на IdP | Phase 1/2 |
| D8 | Синхронизация email при последующих входах | **✅ Подтверждено (B):** обновлять только при `email_verified=true`, непустом и валидном email; unverified/отсутствующий — не трогать; дубли разрешены | Phase 7 |
| D9 | Политика удалённого/анонимизированного аккаунта | **✅ Подтверждено (A):** удаление — статус `deleted/anonymized` в той же событийной схеме, что и бан; локальная анонимизация PII; `_id`/игры сохраняются; вход запрещён; восстановление только по `status=active` | Phase 2 |
| D10 | Reopen-флоу закрытых аккаунтов | **✅ Подтверждено (A):** self-reopen через SSO + appeal-сессию; email-token reopen удаляется; `closedByMod`/`foreverClosed`/global/deleted → отказ | Phase 1/2 |
| D11 | Формат username и `historicalRegex` | **✅ Подтверждено:** `[\p{L}\p{N}_\- ]{2,30}` — буквы/цифры (UTF-8, включая кириллицу), `_`, `-`, пробел; длина 2–30; display-only; `historicalRegex` больше не правило UserId | Phase 3 |
| D12 | Токен или cookie для игрового клиента | **✅ Подтверждено (hybrid):** существующий клиент/WS остаются на `sessionId` с усилением (TTL, привязка, отзыв, WS `enabled`); OAuth Bearer — для API/server-to-server/новых интеграций; WS-token — позже через subprotocol/first-message | Phase 9 |

### Журнал решений

- **D1 — ✅ подтверждено (2026-10-19):** вариант A. App-level guard по флагу `auth.ssoOnly`; все локальные auth-endpoint'ы возвращают 404; `GET /login` и `GET /signup` остаются SSO-входом; `makeLoginToken` закрывается; после стабилизации локальные роуты удаляются из `conf/routes`; nginx — дополнительный слой, не единственный контроль. Reopen-флоу — по D10.
- **D2 — ✅ подтверждено (2026-10-19):** username — алфавитно-цифровой UTF-8 (включая кириллицу), может меняться, уникальность не гарантируется. Используется только для отображения. Unique index на `username` не создаём; для поиска — non-unique index. `byUsername` может возвращать несколько пользователей и не используется для identity. Все адресные операции (PM, report, team, study, plan, mod) переводим на `UserId`. `historicalRegex` больше не правило UserId.
- **D3 — ✅ подтверждено (2026-10-19):** SSO гарантирует неизменность `sub`; переиспользование исключено. `_id = sub` остаётся. Добавляем мониторинг аномалий (один `sub` — разные ключевые атрибуты) и не удаляем tombstone lila-пользователя.
- **D4 — ✅ согласовано (2026-10-19):** IdP остаётся source of truth по глобальному статусу, но **не пишет в БД lila напрямую**. Lila хранит проекцию `identity_status` по `sub`; изменения приходят signed-событиями (HTTP-webhook или существующий брокер) с `eventId`/`version`; при входе проверяется claim `globally_enabled` из ID token/userinfo; при запрете закрываются все сессии; backchannel logout — в Phase 4. Прямая запись IdP в `user4` удаляется, DB-доступ отзывается. Текущие баны импортируются backfill-ом, затем работает reconciliation.
- **D5 — ✅ подтверждено (2026-10-19):** дубли email разрешены; email — необязательные контактные данные, не идентификатор и не основание для входа/линковки. Unique index на `email` убрать, оставить non-unique индекс для поиска. Уникальность email — ответственность IdP (с оговоркой про зоопарк провайдеров); часть провайдеров email не отдаёт. Коммуникации в перспективе выносятся в Discourse, поэтому lila не должна строить на email auth/identity-логику.
- **D6 — ✅ подтверждено (2026-10-19):** только `response_mode=query`. IdP-клиент и `AuthenticationRequest` явно фиксируют query; поддерживается только `GET /signin-oidc`; `POST /signin-oidc` удаляется. `form_post` и другие варианты не рассматриваем.
- **D7 — ✅ подтверждено (2026-10-19):** вариант A — локальная 2FA полностью отключается. Настройки и роуты 2FA скрываются/удаляются; `totpSecret` не участвует в OIDC-входе; MFA — ответственность IdP (включая staff/admin).
- **D8 — ✅ подтверждено (2026-10-19):** вариант B — email синхронизируется при последующих входах только при `email_verified=true`, непустом и валидном значении. Unverified/отсутствующий email не перезаписывает существующее значение и не очищает его. Дубли разрешены, уникальность не проверяется.
- **D9 — ✅ подтверждено (2026-10-19):** вариант A. Удаление/анонимизация в IdP — это статус `deleted/anonymized` в той же событийной схеме, что и глобальный бан. Lila обновляет `identity_status`, закрывает сессии, запрещает вход, анонимизирует локальные PII (username/email/profile), но сохраняет `_id` и игровые записи. Восстановление — только по `status=active` с повторной синхронизацией username/email; старые PII не воскресают. Отдельный локальный tombstone-механизм не нужен.
- **D10 — ✅ подтверждено (2026-10-19):** вариант A. Self-reopen только после SSO-входа через appeal-сессию; `/account/reopen` добавляется в `isClosedLoginPath`; `closedByMod`/`foreverClosed`/global ban/deleted → отказ. Email-token reopen (`/account/reopen/send`, `/account/reopen/login/:token`, `Reopen.send/confirm` как маршруты) удаляется. Reopen-submit — rate limit + аудит; обычная сессия создаётся только после успешного reopen.
- **D11 — ✅ подтверждено (2026-10-19):** username — `[\p{L}\p{N}_\- ]{2,30}`: буквы/цифры (UTF-8, включая кириллицу), `_`, `-`, пробел; длина 2–30. Пробелы разрешены, поэтому username нельзя использовать в URL-путях, mention-парсинге и identity; только display и поиск с выбором. `historicalRegex` перестаёт быть правилом UserId; `UserId` валидируется отдельным numeric-правилом.
- **D12 — ✅ подтверждено (2026-10-19): hybrid.** Текущий клиент (`arena/src`) и `line-ws` остаются на `sessionId`: клиент хранит его в storage, шлёт заголовком `sessionId` в HTTP и query-параметром в WS; `line-ws` читает cookie `live2`/query и проверяет только `security._id + up=true`. Поэтому `sessionId` усиливаем: TTL/expiresAt, закрытие старых сессий при логине, привязка к провайдеру/платформе/игроку, проверка `enabled`/`foreverClosed` в WS, закрытие сессий по D4/D9, отказ от query-логирования. OAuth Bearer — для API/server-to-server и новых платформ; полный OAuth-rewrite клиента/WS не делаем. Если OAuth понадобится для WS — через subprotocol/first-message, с fallback на `sessionId`.

---

## 3. Принципы

1. **UserId — единственный идентификатор.** `UserName` — отображаемое имя, не ключ.
2. **Один provisioning.** OIDC и будущий signature-flow используют один `UserProvisioning`.
3. **Deny by default.** Локальные auth-эндпоинты недоступны, даже если nginx настроен неправильно.
4. **Fail closed.** Ошибки OIDC не превращаются в 500 без обработки; пользователь получает управляемый ответ.
5. **IdP — единственный брокер внешних идентичностей.** Lila не доверяет platform-id и raw signature.
6. **Никаких секретов в логах.** Токены, signatures, коды и email не логируются в открытом виде.
7. **Изменения атомарны по фазам.** Каждая фаза имеет свои тесты и критерии приёмки.

---

## 4. Фазы работ

### Phase 0 — Baseline и подготовка

**Цель:** зафиксировать текущее состояние, точки отказа и тестовый контур.

- [ ] **0.1. Инвентаризация локальных auth-эндпоинтов.**
  - `conf/routes:517-543`: `POST /login`, `POST /signup`, `POST /password/reset/send`, `POST /password/reset/confirm/:token`, `POST /auth/magic-link/send`, `POST /auth/token`, `GET/POST /auth/token/:token`.
  - `conf/routes:528-531` — email-confirm signup: `GET /signup/check-your-email`, `POST /signup/fix-email`, `GET/POST /signup/confirm/:token`. **`signupConfirmEmailPost` выдаёт сессию**; кроме того, `SecurityApi.saveAuthentication` при `mustConfirmEmail=true` уводит OIDC-пользователя в этот локальный флоу (`authRecovery`).
  - GET-страницы локальных флоу: `GET /password/reset`, `GET /password/reset/sent/:email`, `GET /auth/magic-link`, `GET /auth/magic-link/sent`.
  - `conf/routes:773-796` — локальное управление аккаунтом: `/account/passwd` (смена пароля), `/account/email` + `/account/email/confirm/:token` (конфликт с D8), `/account/username` (конфликт с D2 — username из IdP), `/account/delete` (локальный `AccountTermination`, конфликт с D9), `/account/close` (оставить — локальное закрытие, A4; переписать без пароля), `/account/kid` (оставить lila-фичу, переписать без пароля или отключить), `/account/reopen*` (D10).
  - `conf/routes:790-792` — 2FA: `GET /account/twofactor`, `POST /account/twofactor/setup`, `POST /account/twofactor/disable`; `conf/routes:555` — `POST /mod/:userId/disable-2fa`.
  - `conf/routes:793-796`: `/account/reopen`, `/account/reopen/send`, `/account/reopen/sent`, `/account/reopen/login/:token`.
  - `conf/routes:827-830` — personal OAuth-токены (`/account/oauth/token/*`): создаются после SSO-входа, но должны отзываться при бане/удалении (2.5).
  - Для каждого: кто вызывает, есть ли ссылки в views/UI, есть ли мобильные клиенты.
  - Результат: таблица «эндпоинт → обработчик → вызывающие → решение (удалить/guard/оставить)».

- [ ] **0.2. Инвентаризация мест, где username используется как UserId.**
  - Базовый список: `rg -n "\.name\.id|username\.id|\.username\.id" modules app`.
  - Дополнительно: `getUserStr(...).id`, `UserStr.read(...).id`, `UserIdOf[UserName]`, `UserIdOf[DuelPlayer]`.
  - Grep не найдёт неявные использования через `UserIdOf` (`.is(name)`, `.isnt(name)`, `.id` на `UserName`) — полный список даёт компилятор после удаления given'ов (3.1); grep — предварительная оценка объёма.
  - Потребители `UserName.historicalRegex`: `UserStr.read` (`userId.scala:76-78`) → path-binder'ы `LilaRouter.scala`, `UserStr.couldBeUsername`/`validateId` (`userId.scala:68-69`), `Form.scala:266`, `ModInquiryUi.scala:138` — вход для 3.5.
  - Результат: список с классификацией «identity / display / game data».

- [ ] **0.3. Тестовый контур.**
  - Unit-тесты модуля `oidc` (или нового `identity`).
  - Mock IdP для metadata/token/userinfo (WireMock или аналог).
  - Тест на двух пользователей с `id != username` и потенциальной коллизией.
  - Команды приёмки: `sbt compile`, `sbt "oidc/test"`, профильные тесты модулей.

- [ ] **0.4. Feature flag для поэтапного включения.**
  - Добавить конфиг `auth.ssoOnly` (или `auth.local.enabled=false`) в `conf/base.conf` и типизированный доступ.
  - На dev/staging флаг позволяет включить guard до удаления роутов.

**Критерий выхода Phase 0:** есть полный список эндпоинтов и мест с `name.id`, тестовый контур запускается, флаг доступен.

---

### Phase 1 — P0: Закрыть локальный стек аутентификации

**Цель:** ни один локальный endpoint не может создать сессию или credential; SSO-вход не выдаёт сессию закрытому аккаунту.

- [ ] **1.0. Хотфикс: проверка статуса в `Auth.oidcCallback` (до Phase 2).**
  - Сейчас `oidcCallback` (`Auth.scala:106-112`) вызывает `authenticateUser` для любого найденного пользователя: проверок `enabled`/`foreverClosed` нет, `saveAuthentication` проверяет только `mustConfirmEmail`. Важно: HTTP-запросы disabled-пользователя не восстанавливаются (`restoreUser` → `userRepo.me` → `enabledById`), но callback всё равно создаёт сессию, отдаёт mobile JSON, а `line-ws/Auth` принимает её по `up=true` — то есть открытый WS остаётся рабочим.
  - Минимальный фикс: `enabled=false` или `foreverClosed=true` → отказ без сессии (управляемая страница, не 500). Полная логика appeal/reopen — 1.4 и 2.4.
  - `None` из `OidcApi.authenticate` → redirect на login с сообщением вместо `InternalServerError("Authentication error")`.
  - Тест: disabled/foreverClosed пользователь не получает cookie-сессию через `/signin-oidc`.

- [ ] **1.1. Guard на уровне приложения.**
  - Все локальные auth-методы в `app/controllers/Auth.scala` и `app/controllers/Account.scala` при `ssoOnly=true` возвращают `NotFound` (404).
  - Список `Auth`: `authenticate`, `signupPost`, `checkYourEmail`, `fixEmail`, `signupConfirmEmail`, `signupConfirmEmailPost` (**выдаёт сессию**), `passwordReset`, `passwordResetApply`, `passwordResetSent`, `passwordResetConfirm`, `passwordResetConfirmApply`, `magicLink`, `magicLinkApply`, `magicLinkSent`, `loginWithToken`, `loginWithTokenPost`, `makeLoginToken` (закрыть).
  - Список `Account`: `passwd`/`passwdApply`, `email`/`emailApply`/`emailConfirm` (email только из IdP, D8), `username`/`usernameApply` (username только из IdP, D2), `delete`/`deleteConfirm`/`deleteDone` (удаление — через IdP, D9), `twoFactor`/`setupTwoFactor`/`disableTwoFactor` (D7), `reopenApply`, `reopenSent`, `reopenLogin` (reopen — по D10).
  - `close`/`closeConfirm` остаются (локальное закрытие, A4), но **переписываются на SSO-подтверждение без пароля**: `SecurityForm.closeAccount` (`SecurityForm.scala:205-212`) требует `passwd`, `myUsernameField` (`:29-30`) — `UserStr`; у OIDC-пользователей нет usable password. Новая форма: подтверждение (checkbox/display-name), проверка `closedByMod`/`foreverClosed`, затем `accountTermination.disable`. Убрать `passwordMapping`/`HasherRateLimit` для этого пути, оставить CSRF и rate limit.
  - `kid`/`kidPost` остаются lila-фичей, но `toggleKid = passwordProtected` (`SecurityForm.scala:214`) — переписать на passwordless SSO-подтверждение или отключить/скрыть; убрать `passwordMapping`/`HasherRateLimit` и оставить CSRF/rate limit. Не оставлять парольную форму, которая гарантированно не работает.
  - `myUsernameField`/`closeAccount`/`deleteAccount` не должны использовать `UserStr` как идентификатор после 3.5; для display-подтверждения — `UserName`/checkbox, для ID — явный `UserId`.
  - `SecurityApi.saveAuthentication`: при `ssoOnly=true` не проверять `mustConfirmEmail` (или гарантировать `mustConfirmEmail=false` для всех SSO-пользователей), чтобы OIDC-вход не уходил в локальный email-confirm флоу через `authRecovery`.
  - `GET /login` и `GET /signup` остаются точками входа в SSO (`oidcLogin`).
  - `logout` остаётся, но см. Phase 4.

- [ ] **1.2. Удаление роутов после стабилизации.**
  - Удалить из `conf/routes` локальные auth-маршруты.
  - Удалить/скрыть ссылки и формы в views/UI, которые на них ведут.
  - Сохранить SSO-маршруты: `GET /login`, `GET /$lang/login`, `GET /signup`, `GET /$lang/signup`, `GET /signin-oidc`, `logout`; плюс переработанные account-страницы `close`/`kid` и обычные не-auth маршруты приложения.
  - Удалить также маршруты `/account/passwd`, `/account/email*`, `/account/username`, `/account/delete*`, `/account/twofactor*` и пункты меню аккаунта, ведущие на них (смена этих данных — в IdP; при необходимости — ссылка на профиль `passport.chess-online.com`).
  - `POST /signin-oidc` удалить (D6: только query).

- [ ] **1.3. Запрет локальных credentials.**
  - При SSO-only все пользователи внешние, поэтому отдельный маркер `external=true` не нужен: при `ssoOnly=true` парольная аутентификация (`Authenticator`/`loadLoginForm`) всегда отказывает, независимо от наличия `bpass`.
  - `OidcApi.createOidcUser` не создаёт usable password: новый путь `createExternal`/`newUserExternal` без `bpass` (см. 2.2; `create2` не меняем). Существующие случайные `bpass` можно не трогать — они не используются, а парольные формы close/kid переписываются в 1.1.
  - Если понадобится различать провайдеров (Phase 9) — хранить `authProvider` как метаданные, не как условие доступа.
  - Проверка: даже при ошибочно доступном `POST /login` вход по паролю невозможен.

- [ ] **1.4. Reopen-флоу (D10 A).**
  - `Auth.oidcCallback` при `enabled=false`:
    - `closedByMod`/`foreverClosed`/global ban/deleted → отказ, без сессии;
    - self-closed → appeal-сессия (`Auth.authenticateAppealUser`-механика) и redirect на `/account/reopen`.
  - Добавить `/account/reopen` в `HTTPRequest.isClosedLoginPath`, чтобы `restoreUser` поднимал `AppealUser`.
  - `/account/reopen`: форма подтверждения без email-token; повторные проверки `closedByMod`/`foreverClosed`; `userRepo.reopen` + `reportApi.reopenReports`; закрыть appeal-сессию и создать обычную сессию только после успешного reopen.
  - Удалить/закрыть `/account/reopen/send`, `/account/reopen/login/:token`, `Reopen.send`/`Reopen.confirm` как маршруты.
  - Reopen-submit: rate limit + аудит.
  - Тесты: self-closed reopen работает; closedByMod/foreverClosed/global/deleted — отказ; повторный reopen; нет email-токена.

- [ ] **1.5. Тесты.**
  - Каждый локальный endpoint при `ssoOnly=true` → 404, сессия не создаётся.
  - OIDC-вход работает.
  - OIDC-пользователь не может воспользоваться password reset/magic link.
  - `GET /login` и `GET /signup` ведут в SSO.

- [ ] **1.6. Отключить локальную 2FA (D7).**
  - Скрыть/удалить страницы и роуты настройки 2FA: `GET /account/twofactor`, `POST /account/twofactor/setup`, `POST /account/twofactor/disable` (`conf/routes:790-792`), соответствующие views/ссылки в меню аккаунта.
  - `totpSecret` не проверяется при OIDC-входе и не должен появляться у новых аккаунтов.
  - `Mod.disableTwoFactor` — удалить или оставить только как cleanup-операцию.
  - MFA/staff-admin policy — на IdP.
  - Тест: пользователь с искусственно установленным `totpSecret` входит через SSO без локального TOTP (ожидаемо при A), и включить локальную 2FA через UI нельзя.

- [ ] **1.7. Обязательная проверка подписи ID token.**
  - `new IDTokenValidator(issuer, clientID)` (`OpenIDConnectService.scala:205`, ветка без `jws_alg`) — валидатор для **неподписанных (plain)** ID token (подтверждено по исходникам Nimbus 11.23). Сейчас `Env` всегда передаёт `Some(jwsAlgorithm)` и в `base.conf` задан `RS256`, поэтому plain-ветка достижима только через тесты/будущие вызовы, но API позволяет её случайно включить. Убрать fallback и сделать `jws_alg` обязательным.
  - `jws_alg` сделать обязательным: `OIDC.jwsAlgorithm: JWSAlgorithm` (не `Option`); пустое значение/неизвестный алгоритм → ошибка на старте. Fallback на plain-валидатор удалить.
  - Разрешать только асимметричные алгоритмы, объявленные в `id_token_signing_alg_values_supported` discovery (например, RS256/ES256); `none` и HS* запрещены.
  - Тест: ID token с `alg=none`/неверной подписью отклоняется; старт без `jws_alg` падает с понятной ошибкой.

**Критерий выхода Phase 1:** в приложении нет способа получить сессию/credential без SSO; попытка локального входа не создаёт cookie-сессию; SSO-вход не выдаёт сессию закрытому аккаунту; `close`/`kid` работают без пароля; подпись ID token проверяется всегда.

---

### Phase 2 — P0: UserProvisioning, баны и удалённые аккаунты

**Цель:** один provider-agnostic путь `ExternalIdentity → User`, с корректной обработкой статусов.

- [ ] **2.1. Выделить модель `ExternalIdentity`.**
  - Рекомендуемое расположение: новый модуль `modules/identity` (зависит от `common`, `db`, `user`), чтобы его могли использовать `oidc` и будущий game-flow без циклов зависимостей.
  - Допустимый минимум: `modules/oidc/src/main/UserProvisioning.scala` с последующим выносом.
  - Инфраструктура: добавить модуль в `build.sbt` (dependsOn `common`, `db`, `user`), подключить в `app/Env.scala`; `oidc` и `oauth` должны зависеть от `identity` (без цикла: `identity` не зависит от `security`/`oauth`). Без wiring `UserProvisioning`/`IdentityStatusService` не будут доступны приложению.
  - Чтобы не создать цикл `security → oidc → identity → security`, `IdentityStatusService` не должен напрямую вызывать `security.store`/`OAuthServer`. Отзыв credentials — через порт `CredentialRevoker` (реализация в `security`/`app`) или bus-события (`CloseAccount`, `RevokeOAuthTokens`), которые слушают `security`/`oauth`.
  - Поля:
    ```scala
    case class ExternalIdentity(
        issuer: String,
        subject: String,          // канонический sub
        username: UserName,
        email: Option[EmailAddress],
        emailVerified: Boolean,
        globallyEnabled: Option[Boolean]
    )
    ```

- [ ] **2.2. Реализовать `UserProvisioning.resolve`.**
  - `byId(sub)` (не `enabledById`), затем явная проверка:
    - `enabled=false` → `Left(Banned/Closed)`;
    - `foreverClosed=true` → `Left(ClosedForever)`;
    - tombstone/удалённый → `Left(Deleted)`;
    - нет пользователя → создать.
  - Создание идемпотентно: duplicate `_id` → перечитать пользователя; duplicate email **не является ошибкой** (D5) — email просто контактные данные.
  - Не менять сигнатуру `UserRepo.create2`/`newUser2` (`UserRepo.scala:252`), чтобы не ломать локальный signup до его удаления. Добавить отдельный `createExternal`/`newUserExternal`: `email: Option[EmailAddress]`, создание без `bpass` (1.3), `mustConfirmEmail = false`. Текущий `EmailAddress(userInfo.getEmailAddress)` в `OidcApi.createOidcUser` получает `null` при отсутствии claim — заменить на `Option`.
  - `create2` при существующем `_id` возвращает `None` (`exists(id).not.flatMapz`) — в гонке параллельного первого входа это сейчас даёт 500. `resolve` должен в этом случае перечитать пользователя.
  - Валидация: `sub` непустой; username непустой и соответствует Unicode-формату SSO (буквы/цифры, включая кириллицу); email отсутствует или валиден. Email не участвует в identity-проверках и не блокирует вход.
  - Обновление username — по политике D2 (display-only, без уникальности); обновление email — только при `email_verified=true` и непустом/валидном значении (D8); unverified/отсутствующий email не перезаписывает и не очищает. Username/email не используются как ключи; инвалидировать `LightUser`-кэш.
  - `globallyEnabled=Some(false)` → отказ.
  - Возвращать типизированный результат, а не `Option[User]`, чтобы контроллер мог выбрать правильный ответ.
  - Типизированные ошибки OIDC-потока (`InvalidState`, `ProviderError`, `TokenError`, `ClaimsError`) вводятся здесь же вместе со статусами provisioning (`Banned`, `Closed`, `ClosedForever`, `Deleted`) — единый ADT `LoginError`. Phase 5.4 только дополняет маппинг/тексты.

- [ ] **2.3. Перевести `OidcApi` на `UserProvisioning`.**
  - Удалить `getOrCreateUser`/`createOidcUser` из `OidcApi` или оставить тонкими адаптерами.
  - `OidcApi.authenticate` возвращает `ExternalIdentity` + токены/claims, а не `Option[User]`.

- [ ] **2.4. Обработка статусов в `Auth.oidcCallback`.**
  - `enabled=false` → closed/appeal/reopen-флоу по D10: self-closed получает appeal-сессию и `/account/reopen`; closedByMod/foreverClosed/global/deleted — отказ. Обычная сессия не создаётся до успешного reopen.
  - `foreverClosed` → отказ с понятным сообщением.
  - `Deleted` → сообщение/политика D9.
  - `globallyEnabled=false` → отказ.
  - Ошибки claims/провайдера → 4xx/redirect на login с сообщением, не 500.

- [ ] **2.5. Глобальный бан: projection + signed events (D4).**
  - **Убрать прямую запись IdP в БД lila**; отозвать DB-доступ у IdP.
  - Добавить коллекцию `identity_status` (`_id = sub`, `globallyEnabled`, `status`, `version`, `occurredAt`, `reasonCode`, `updatedAt`). Не смешивать с `user4.enabled`/`foreverClosed`.
  - При входе `UserProvisioning` проверяет `identity_status` и/или claim `globally_enabled` из ID token/userinfo.
  - Принять signed-события IdP:
    - `eventId` (уникальный), `occurredAt`, `issuer`, `sub`, `status`, `version`, `reasonCode`;
    - endpoint `POST /internal/identity/events` (internal network, HMAC/mTLS, без CSRF) или существующий брокер;
    - идемпотентность по `eventId`, порядок по `version`/`occurredAt`, уникальный индекс.
  - `IdentityStatusService.apply(event)`: обновить проекцию → при запрете отозвать все credentials пользователя (через `CredentialRevoker`/bus, без цикла зависимостей) → аудит (`source=idp`, без PII) → внутреннее событие.
  - При запрете **погасить активные WS**: опубликовать `lila.core.security.CloseAccount(userId)`, который `RemoteSocket` превращает в `disconnect/user` (`RemoteSocket.scala:92-93`), а `line-ws` кикает (`LilaHandler.scala:33`). `closeAllSessionsOf` закрывает только будущие handshake, но не открытые сокеты.
  - `OAuthServer.auth` (`OAuthServer.scala:43-44`) сейчас проверяет только `userApi.me` (`enabled`); добавить проверку `identity_status` для global ban/deleted (через кэшируемый shared-сервис), иначе personal/access token продолжит работать при потерянном событии/гонке revocation.
  - `line-ws/Auth` должен проверять `identity_status`/`enabled`/`expiresAt` на handshake; при открытом сокете — периодический check или kick по `CloseAccount` (P0).
  - «Отозвать все credentials» — это не только `security.store.closeAllSessionsOf`, но и:
    - appeal-сессии (отдельный store с префиксом, `SecurityApi.scala:238`);
    - personal OAuth-токены (`modules/oauth`, `/account/oauth/token/*`) и OAuth-токены сторонних приложений;
    - web push-подписки по сессиям (`webSubscriptionApi`).
  - То же для локального бана/закрытия (`user4.enabled=false`): проверить, что существующий путь закрытия отзывает OAuth-токены, иначе API остаётся доступным.
  - Backfill: импортировать текущий список глобально забаненных `sub` из IdP.
  - Reconciliation: периодическая сверка проекции с IdP (signed snapshot/since) на случай потерянных событий.
  - Инфраструктура: `bin/mongodb/indexes.js` — `identity_status` (`_id=sub`, `version`, `status`), `identity_event` (`_id=eventId`, unique; опционально TTL), `security.sid` (sparse), `security.expiresAt`; `conf/routes` — `POST /internal/identity/events` (internal only, CSRF-exempt); контроллер/сервис в новом модуле или `security`.
  - `IdentityStatusService.apply` сначала пишет/проверяет `identity_event` по `eventId` (идемпотентность), затем обновляет `identity_status` по `version`; старые/повторные события игнорируются.
  - Разбан — событие `status=active`; локальный бан не снимается автоматически.
  - Проверка: забаненный на IdP пользователь не получает lila-сессию; уже выданные сессии закрываются.

- [ ] **2.5a. Удаление/анонимизация (D9).**
  - `deleted/anonymized` — такое же signed-событие, как бан; `IdentityStatusService` обновляет `identity_status`.
  - Закрыть все сессии и погасить активные WS (`CloseAccount`/`disconnectUser`); `UserProvisioning` возвращает `Deleted`; вход запрещён, 500 нет.
  - Локальная анонимизация PII: username/email/profile; сохранить `_id`, игры и ссылочную целостность. Не запускать полный `AccountTermination`.
  - Восстановление — только по `status=active`: вход разрешается, username/email синхронизируются из IdP, старые PII не восстанавливаются.
  - Анонимизация должна удалять и `prevEmail`: `UserRepo.reopen` (`UserRepo.scala:350-358`) переименовывает `prevEmail → email`, и без этого локальный reopen вернул бы анонимизированный email.
  - Не создавать нового пользователя на тот же `sub`; `_id`/tombstone сохраняются.
  - Тесты: deleted → сессии закрыты, вход запрещён, повторный вход не создаёт аккаунт и не даёт 500; active после deleted → вход работает с новыми username/email; PII не воскресают.

- [ ] **2.6. Мониторинг аномалий `sub` (D3 подтверждён).**
  - `sub` immutable и non-reused по контракту SSO; `_id = sub` остаётся.
  - Добавить лог/алерт: если для одного `sub` приходят разные ключевые атрибуты личности (например, `identityId`/`createdAt`), это сигнал о нарушении контракта.
  - Не удалять tombstone lila-пользователя: `create2` не должен создавать нового пользователя на тот же `_id`.
  - Не использовать `sub` после удаления SSO-аккаунта как признак нового пользователя.

- [ ] **2.7. Тесты.**
  - disabled, foreverClosed, tombstone, deleted/anonymized, missing user, duplicate `_id`, duplicate email, invalid email, missing username, global ban claim.
  - Параллельный первый вход одного пользователя: без 500 и без дубля.
  - Глобальный бан/deleted: сессии, appeal-сессии и OAuth-токены отозваны; API с personal token → 401.
  - Повторный вход: username/email не перезаписываются без необходимости.

**Критерий выхода Phase 2:** все входы используют единый provisioning; баны и удалённые аккаунты обрабатываются предсказуемо; отзыв затрагивает сессии, активные WS, appeal-сессии и OAuth-токены; нет 500 на штатных негативных сценариях.

---

### Phase 3 — P0: Разделение `UserId` и `UserName`

**Цель:** устранить неявные конверсии username → id и все wrong-target сценарии.

- [ ] **3.1. Убрать неявные конверсии.**
  - Удалить `given UserIdOf[UserName]` в `modules/core/src/main/userId.scala:57`.
  - Удалить `given UserIdOf[DuelPlayer]` в `modules/tournament/src/main/Duel.scala:25`.
  - Сделать `UserStr` явным типом: либо `UserId`, либо строка, которую нельзя автоматически превратить в `UserId`.
  - Убрать/пометить deprecated `.id` у `UserStr`/`UserName`; компилятор должен показать все места.

- [ ] **3.2. Добавить поиск по username, не идентификацию.**
  - `def searchByUsername(name: UserName): Fu[List[User]]` (или `byUsername` с явным именем `search...`), потому что уникальность не гарантируется (D2).
  - Non-unique индекс на `username` в `bin/mongodb/indexes.js` — только для поиска/автокомплита, не для identity.
  - Поиск учитывает UTF-8, пробелы, `_`, `-`; регистронезависимость — на уровне поиска, не identity.
  - `UserSearch` (`userId.scala:84-87`) сейчас: regex `(?i)[a-z0-9][a-z0-9_-]{2,28}` (только ASCII) и `takeWhile(' ' !=)` (обрезка на первом пробеле). Поиск по кириллице и по именам с пробелом не работает — переписать под display-правило D11 (Unicode-префикс, внутренние пробелы сохраняются, `trim` по краям). Проверить потребителей: автокомплит, `User.scala` search, `ModUserSearch`, `MsgSearch`.
  - Запрещено использовать результат поиска как однозначный `UserId`: при нескольких совпадениях UI обязан дать выбрать пользователя.
  - Unique index на `username` **не создаём**.

- [ ] **3.3. Исправить границы (controllers/forms/query): везде требуется `UserId`.**
  - Общее правило: username не уникален, поэтому **нельзя** автоматически резолвить имя в одного пользователя. Адресные операции принимают `UserId`; если UI принимает username — это поиск с выбором конкретного результата (id).
  - `app/controllers/Mod.scala:42` — `withSuspect(username.id)` → параметр `UserId` или выбор из результатов поиска.
  - `app/controllers/Report.scala:140,160`, `modules/report/.../ReportForm.scala:52` — репорт на `UserId`; username только для поиска/выбора.
  - `app/controllers/Msg.scala:48`, `modules/msg/.../MsgApi.scala:78,101`, `modules/msg/.../MsgCompat.scala:89,113` — перевести inbox-маршруты на `UserId`; legacy username-маршруты либо удалить, либо оставить только поиск с выбором (D2).
  - `modules/team/.../TeamSecurity.scala:48,53,102` — `LeaderData`/формы работают с `UserId`; `addLeader`/permissions принимают выбранного пользователя (id), не имя.
  - `modules/study/.../StudySocket.scala:115-116`, `app/controllers/Study.scala:519` — `UserId`.
  - `modules/clas/.../ClasForm.scala:92` — `UserId`.
  - `modules/plan/.../Env.scala:83-87` (CLI) — `UserId`; при вводе имени — список совпадений.
  - Query-параметры: `Api.scala:172,225`, `Game.scala:151`, `Round.scala:123`, `Streamer.scala:176`, `Swiss.scala:64`, `Tournament.scala:104`, `User.scala:535`, `Account.scala:390` — перевести на `UserId` или поиск+выбор.
  - `app/controllers/User.scala:68` — search по username возвращает несколько результатов; не делать `UserStr.read(...).id` и не редиректить на одного пользователя без выбора.
  - `modules/user/.../UserForm.scala:24` — локальная смена username: при SSO-владельце запретить; если разрешена — только display-поле, без `name.id`.

- [ ] **3.4. Исправить доменные модели.**
  - `Duel.scala:17` — `userIds` из `player.userId`, а не `name.id`.
  - `TournamentLilaHttp.scala:76`, `JsonView.scala:355` — использовать `UserId`.
  - `MsgCompat` — legacy-эндпоинты: либо перевести на `UserId`, либо явно resolve username.
  - Проверить `ActivityReadApi.scala:161` и аналогичные места с `idName.id`.

- [ ] **3.5. Валидация username как display-поля (UTF-8, D11).**
  - `UserName.historicalRegex` (`userId.scala:59`) сейчас `[1-9][0-9]{0,30}` — фактически правило UserId, а не username. **Простая замена regex недопустима:** он используется для разбора идентификаторов в URL, и после замены `UserStr.read` начнёт принимать кириллицу/пробелы, а `.id` превратит их в `UserId` через lowercase.
  - Разделить на два правила:
    - `UserId.regex` — numeric `[1-9][0-9]{0,30}` **плюс whitelist системных id** (`lichess`, `ghost`, `broadcaster`, `irwin`, `kaladin`, `openingexplorer`, `lichess4545`, `challengermode`, `watcherbot` — `userId.scala:16-25`) и ghost-id с префиксом `!` (`isGhost`). Без whitelist системные аккаунты перестанут резолвиться.
    - `UserName.displayRegex` — `[\p{L}\p{N}_\- ]{2,30}`: буквы/цифры (включая кириллицу), `_`, `-`, пробел; длина 2–30.
  - Перевести потребителей явно:
    - `UserStr.read` (`userId.scala:76-78`) и path-binder'ы в `modules/ui/src/main/router/LilaRouter.scala` → `UserId.regex`;
    - `UserStr.couldBeUsername`/`validateId` (`userId.scala:68-69`) → `UserId.regex` (переименовать в `couldBeUserId`);
    - `Form.scala:266` (`username.historicalField`) → `displayRegex` только для display-полей; поля, принимающие идентификатор, — отдельный `userIdField` с `UserId.regex`;
    - `ModInquiryUi.scala:138` → см. ниже.
  - Тест: `/@/1001` резолвится; `/@/lichess`, `/@/ghost` резолвятся; `/@/Александр` и `/@/Иван%20Петров` → 404, а не lowercase-id.
  - `Form.username.historicalField` (`modules/common/.../Form.scala:262-268`) должен валидировать username по display-правилу; `trim` по краям, внутренние пробелы сохранять; отклонять control-символы/переводы строк. ID-валидация (`UserId`, numeric) — отдельно.
  - `ModInquiryUi.scala:138` — regex для `@username` сделать Unicode-aware с учётом пробелов; если парсинг mention по имени становится неоднозначным — использовать `UserId`/выбор пользователя.
  - Пробелы в username означают, что username нельзя использовать в URL-путях, mention-парсинге и identity. Только display и поиск с выбором.
  - `myUsernameField`/`closeAccount`/`deleteAccount` (`SecurityForm.scala:29-30,205-234`) сейчас используют `UserStr` и сравнение с `me`; после переопределения `UserStr` это сломает формы. Переписать на display-name/checkbox или явный `UserId` (см. 1.1).
  - `UserStr` после переопределения фактически означает ID-строку; переименовать/задокументировать (например, `UserIdStr`/`UserRef`), чтобы будущий код не использовал его как display-username.
  - Уникальность не проверяется (D2). Не использовать username в `UserIdOf`, `UserStr.id`, ключах кэша, правах и адресации.

- [ ] **3.6. Побочные эффекты смены username.**
  - `UserRepo.setUsername` (`UserRepo.scala:164-165`) обновляет только поле `username`; добавить:
    - проверку формата (не уникальности);
    - обновление `len`;
    - инвалидацию `LightUser`-кэша;
    - событие/публикацию, если требуется;
    - ожидание результата (убрать fire-and-forget в `OidcApi`).
  - `OidcApi` не должен вызывать `setUsername` без `flatMap`; `null`/пустой `preferred_username` не записывается.
  - `UserRepo.setUsernameCased` (`UserRepo.scala:167-177`) сравнивает `id.is(name)` и при `id != username` всегда падает. Удалить вместе с `/account/username` (1.1) — username приходит только из IdP.
  - При дублирующихся username UI должен показывать различающий `UserId`/дополнительный признак.

- [ ] **3.7. Тесты.**
  - A: `id=1001`, `username="Александр"`; B: `id=2002`, `username="Александр"` (одинаковые username).
  - Прогнать: PM, report, team addLeader/permissions, study kick, tournament duel, plan CLI, search.
  - Все операции должны адресоваться правильному `UserId`, даже при одинаковых username.
  - Тест `username == чужой _id`: операция не должна затрагивать чужой аккаунт.
  - Тест кириллицы/UTF-8: отображение, поиск, формы.
  - Тест username с пробелом, `_`, `-` (например, `Иван Петров_01`): отображение, поиск, формы; URL/mention по имени не используются.
  - Тест: поиск по неуникальному username возвращает несколько результатов и требует выбора, а не редиректит на первого.

- [ ] **3.8. Cross-repo: клиент `arena/src` и `line-ws`.**
  - Аудит `arena/src` на использование username как ключа/идентификатора: `{[username: string]: LightUser}` в `ui/timeline.tsx`/`interfaces/index.ts`, `CorrespondenceSeek.username`, PM/inbox, чаты/игры, поиск.
  - Перевести клиент на `LightUser.id`/`Session.id` там, где нужен идентификатор; username — только display.
  - Изменение inbox/PM-маршрутов на `UserId` требует синхронного изменения клиента; предусмотреть dual-route/период совместимости или отдельный релиз.
  - `line-ws` — отдельный репозиторий: изменения `Auth.scala`/`Mongo.scala`/`LilaHandler.scala` (TTL/`enabled`/`identity_status`/disconnect) оформить отдельным PR и синхронным деплоем.
  - Приёмка: `arena` собирается/проходит тесты; `line-ws` проходит свои тесты; ручной сценарий клиент↔lila↔WS.

**Критерий выхода Phase 3:** в identity-путях нет `name.id`/`username.id`; компилятор не позволяет неявно превратить имя в id; все адресные операции работают с `UserId` и проходят collision-тест; одинаковые username, кириллица, пробелы, `_`/`-` не ломают адресацию (поиск даёт выбор, URL/mentions — по `UserId`).

---

### Phase 4 — P0: Logout, backchannel и жизненный цикл сессии

**Цель:** «выход» завершает SSO-сессию; глобальный бан/завершение сессии на IdP инвалидирует lila-сессию.

- [ ] **4.1. Сохранять ID token/claims.**
  - `OpenIDConnectService.obtainOIDCToken` сейчас возвращает только `BearerAccessToken`; вернуть структуру с ID token/claims (или отдельный `OidcSessionData`).
  - Хранить **server-side** в документе `security` (не в cookie): `idToken` (для `id_token_hint`), `sid` (claim из ID token), `iss`, `authTime`. Без `sid` маппинг backchannel logout на конкретную сессию (4.3) невозможен — останется только «убить все сессии `sub`».
  - Индекс `security.sid` (sparse) и `security.expiresAt` в `bin/mongodb/indexes.js`.
  - `idToken` — чувствительный артефакт: ограничить доступ к коллекции, не логировать, по возможности шифровать at rest.
  - Не логировать токены.

- [ ] **4.2. RP-initiated logout.**
  - `Auth.logout` (`Auth.scala:173-180`): удалить локальную сессию, затем redirect на `end_session_endpoint` с:
    - `id_token_hint` (если хранится);
    - `post_logout_redirect_uri`;
    - `client_id`, `state` при необходимости.
  - Не редиректить на `/login` сразу после logout — иначе SSO снова залогинит.
  - JSON/мобильные клиенты (`negotiate`-ветка `logout`, `logoutGet` — `Auth.scala:173-190`): редирект на `end_session_endpoint` неприменим. Удалять локальную сессию и возвращать `jsonOkResult` + (опционально) URL `end_session_endpoint` для клиента; SSO-сессию завершает клиент/IdP.

- [ ] **4.3. Backchannel logout endpoint.**
  - Добавить маршрут для logout token от IdP.
  - Валидировать подпись, `iss`, `aud`, `events`, `jti`, время.
  - Маппинг `sub`/`sid` → локальные сессии; удалить их (`closeAllSessionsOf`/аналог).
  - Отвечать корректно на invalid token.

- [ ] **4.4. Frontchannel logout (опционально).**
  - Если нужен веб-сценарий — iframe/endpoint по discovery.

- [ ] **4.5. Глобальный бан и живые сессии (D4).**
  - Событие `identity_status` при `status=banned/suspended/deleted` немедленно закрывает все lila-сессии пользователя (Phase 2.5) и публикует `CloseAccount` для активных WS.
  - IdP дополнительно инициирует backchannel logout; lila принимает logout token.
  - Reconciliation проверяет, что нет активных сессий у `globallyEnabled=false`.
  - Для игровых токенов — короткий TTL + revocation (Phase 9).

- [ ] **4.6. Усиление `sessionId` (D12 hybrid).**
  - Добавить в `security` документ `expiresAt`/TTL и проверять его в HTTP (`restoreUser`/`Store.authInfo`) и WS (`line-ws/Auth`). Дефолты: 30 дней для `remember`, 1 день для session-cookie, 30 дней для mobile/game; значения — в конфиг.
  - При новом логине закрывать старые сессии пользователя/устройства или ограничивать их количество (`closeUserExceptSessionId`/cap), чтобы старые `sessionId` не жили вечно.
  - Хранить в сессии `provider`/`platform`/`playerId` (например, Yandex unique ID) и проверять при восстановлении; не полагаться только на клиентский `PLAYER_ID_KEY`.
  - `line-ws/Auth`: кроме `up=true`, проверять `enabled`/`foreverClosed`/`expiresAt`/`identity_status` (defense in depth).
  - Закрывать сессии и активные WS по D4/D9 событиям: `closeAllSessionsOf` + `CloseAccount`/`disconnectUser` (2.5); для локального бана уже используется `AccountTermination` → `CloseAccount`.
  - `SecurityApi.reqSessionId` (`SecurityApi.scala:193-200`) сейчас предпочитает cookie, а не header. Для mobile/game, который шлёт `sessionId` header, stale cookie может перебивать актуальный id — зафиксировать приоритет header для mobile API или очищать/обновлять cookie.
  - WS: по возможности убрать `sessionId` из query (subprotocol/cookie); если query остаётся — исключить логирование query, короткий TTL, ротация при reconnect. TTL на handshake не закрывает уже открытый сокет — нужен periodic check или kick по событию.
  - `line-ws` — отдельный репозиторий: оформить изменения `Auth.scala`/`Mongo.scala`/`LilaHandler.scala` отдельным PR и синхронным деплоем.
  - Origin/rate-limit проверки для WS handshake.

- [ ] **4.7. Тесты.**
  - Logout → SSO-сессия завершена, повторный `/login` требует аутентификации.
  - Backchannel logout убивает локальную сессию.
  - Невалидный/просроченный logout token отклоняется.
  - Забаненный на IdP пользователь теряет сессию.
  - Просроченный `sessionId` отклоняется HTTP и WS.
  - Повторный логин закрывает/ограничивает старые сессии.
  - WS не пускает disabled-пользователя даже при `up=true`.

**Критерий выхода Phase 4:** logout не приводит к повторному автоматическому входу; backchannel работает; глобальный бан закрывает сессии и активные WS; `sessionId` имеет TTL, привязан к платформе и проверяется в WS.

---

### Phase 5 — P1: Корректность OIDC

**Цель:** привести запросы и разбор ответов в соответствие с IdP и OIDC-спекой.

- [ ] **5.1. Scopes.**
  - Привести `conf/base.conf` к поддерживаемым: `openid profile email` (+ `phone` при необходимости).
  - Сейчас `scopes` из конфига **игнорируются**: в `OpenIDConnectService.scala:31-38` захардкожены `openid email profile address` + `profile_ex`. Исправлять и конфиг (`base.conf:37`), и код.
  - Прокинуть `OidcConfig.scopes` в `OIDCFactory`/`OpenIDConnectService`; убрать hardcode `address`/`profile_ex`. Тот же scope сейчас передаётся и в `TokenRequest` (`:174`) — для authorization_code grant он не нужен, убрать.
  - Валидировать scopes против `scopes_supported` (или зафиксировать контракт с IdP).
  - Проверка: authorization request не содержит неподдерживаемых scopes.

- [ ] **5.2. Claims и userinfo.**
  - Требовать непустой `sub`; проверять null для `email`, `preferred_username`.
  - `email_verified`: политика D5/D8; не блокировать вход в доверенном IdP без необходимости, но не сохранять unverified email как основной.
  - Обрабатывать JWT-userinfo (`getUserInfoJWT`), а не только JSON.
  - Сверять `sub` из ID token и `sub` из userinfo; при расхождении — отказ.
  - Проверять `iss`/`aud`/`exp`/`nonce` (уже есть; добавить тесты).

- [ ] **5.3. `response_mode=query` (D6).**
  - Явно зафиксировать `response_mode=query` в IdP-клиенте и в `AuthenticationRequest`.
  - Оставить только `GET /signin-oidc`; удалить `POST /signin-oidc`.
  - `OidcApi.authenticate` читает только `req.queryString`; `form_post` не поддерживается и не планируется.
  - Проверка: authorization request содержит `response_mode=query`; POST-роут отсутствует; callback работает через GET.
  - `form_post`/fragment/другие режимы не реализовывать.

- [ ] **5.4. Ошибки.**
  - ADT `LoginError` вводится в Phase 2.2 (нужен для 2.4); здесь — перевод всех `fufail(String)` в `OpenIDConnectService` на него и финальный маппинг:
    - `InvalidState`, `ProviderError`, `TokenError`, `ClaimsError`, `Banned`, `Deleted`.
  - `Auth.oidcCallback` маппит их в 4xx/redirect; логирует без секретов.
  - Убрать «сырой» 500 и вводящие в заблуждение сообщения (например, «must have an email claim» при проверке `sub`).

- [ ] **5.5. state/nonce.**
  - Сейчас в flash (`Auth.scala:96-100`, `OidcApi.scala:26-27`) — один набор на браузер, параллельные вкладки ломаются.
  - Хранить state/nonce server-side (короткоживущий кэш по state) или в отдельной cookie с привязкой к flow; single-use, TTL.
  - **PKCE (S256):** генерировать `code_verifier`, передавать `code_challenge`/`code_challenge_method=S256` в `AuthenticationRequest` и `code_verifier` в `TokenRequest`; хранить verifier рядом со state (тот же single-use кэш). Проверить `code_challenge_methods_supported` в discovery.
  - Если IdP отдаёт `iss` в authorization response (RFC 9207, `authorization_response_iss_parameter_supported`) — сверять с ожидаемым issuer.
  - Referrer (7.2) хранить в той же server-side записи flow.
  - Тест: две параллельные вкладки логина; повторное использование state отклоняется; callback без/с неверным `code_verifier` отклоняется IdP.

- [ ] **5.6. Rate limit.**
  - Ограничить `/login` (инициация) и `/signin-oidc` (callback) по IP; не давать анонимному пользователю бесконечно триггерить исходящие запросы.
  - Учесть ошибки/повторы.

- [ ] **5.7. Тесты.**
  - state mismatch, missing code, provider error, expired token, wrong nonce, missing claims, JWT userinfo, unsupported scope.
  - Все негативные сценарии — управляемый ответ, без 500.

**Критерий выхода Phase 5:** OIDC-поток соответствует discovery IdP; негативные сценарии обрабатываются; scopes корректны; multi-tab работает.

---

### Phase 6 — P1: Производительность и устойчивость

**Цель:** убрать блокирующий I/O с потока запроса и лишние сетевые вызовы.

- [ ] **6.1. Singleton/caching `OpenIDConnectService`.**
  - Сейчас `new OpenIDConnectService` на каждый вызов (`OidcApi.scala:20,29`), `metadata` — `lazy val` на экземпляр.
  - Сделать сервис singleton в `Env` или кэшировать `OIDCProviderMetadata` с TTL.
  - Проверка: один well-known fetch на TTL-окно, а не на каждый логин.

- [ ] **6.2. Кэш JWKS/валидатора.**
  - Не создавать `DefaultResourceRetriever`/`IDTokenValidator` на каждый логин; кэшировать JWK set/validator, обновлять по TTL/kid.

- [ ] **6.3. Таймауты.**
  - Явные connect/read timeouts для token и userinfo запросов (сейчас только JWKS).
  - Ограничить общее время логина.

- [ ] **6.4. Блокирующие вызовы.**
  - `OIDCProviderMetadata.resolve` и `request.toHTTPRequest.send()` (`OpenIDConnectService.scala:260,152,237`) — синхронный I/O.
  - Вынести на отдельный ограниченный dispatcher или использовать асинхронный HTTP-клиент.
  - Не блокировать Play dispatcher.

- [ ] **6.5. Гигиена.**
  - Убрать неиспользуемый `akka.stream.Materializer` из `OidcApi`/`Env`, если он не нужен.
  - Метрики/логи ошибок OIDC без токенов/PII.

- [ ] **6.6. Нагрузочный тест.**
  - Параллельные логины, медленный/недоступный IdP; проверить отсутствие thread starvation и рост задержек.

**Критерий выхода Phase 6:** нет блокирующих вызовов на request dispatcher; metadata/JWKS кэшируются; таймауты конечны.

---

### Phase 7 — P2: Email, UX и диагностика

- [ ] **7.1. Email-политика (D5).**
  - Email — необязательные контактные данные, не identity. Null/empty: не падать, не писать пустую строку, не создавать поле.
  - Убрать unique index на `email` в `bin/mongodb/indexes.js`; добавить non-unique индекс для поиска. Миграция: drop unique → create non-unique.
  - Duplicate email: разрешён, не ошибка, не линковать аккаунты по email, не блокировать вход.
  - `UserRepo.byEmail`/`enabledWithEmail`/`idByAnyEmail` сейчас возвращают `Option`; при дублях это неоднозначно. Перевести на `List` или ограничить использование (ModUserSearch — список; auth/reopen — не использовать).
  - Локальный signup/password reset/magic link выключены (D1), поэтому email-поиск не должен влиять на вход.
  - `email_verified`: можно хранить как метаданные, но не использовать для identity/входа; коммуникации в перспективе — Discourse.
  - Синхронизация email при последующих входах (D8 B): обновлять только при `email_verified=true`, непустом и валидном email; unverified/отсутствующий — оставить старое значение. Без влияния на identity и без проверки уникальности.
  - Тесты: несколько пользователей с одинаковым email; пользователь без email; пустой email; mod-search по email возвращает несколько.

- [ ] **7.2. Referrer.**
  - `referrer` сохраняется во flash (`Auth.scala:97`), но читается только из query/session (`Auth.scala:33-36`).
  - Читать flash в OIDC-callback или перенести выбор referrer в одну точку.

- [ ] **7.3. Диагностика.**
  - `getMetadata` теряет причину ошибки (`OpenIDConnectService.scala:262-263`) — логировать cause.
  - Исправить текст ошибки про `sub`/email (`:135`).

- [ ] **7.4. Конфиг.**
  - `jws_alg` — обязательный, перенесено в 1.7 (P0).
  - `client_secret` пустой в default-конфиге: валидировать на старте/при первом использовании с понятной ошибкой.
  - Проверить, что `net.baseUrl` и callback URL совпадают с зарегистрированным redirect URI.

- [ ] **7.5. UX закрытых/удалённых.**
  - Понятные страницы для disabled/foreverClosed/deleted/anonymized; ссылка на appeal/reopen — только по политике D10.
  - Для `deleted/anonymized` — нейтральное «аккаунт недоступен», без раскрытия причины.
  - Не показывать «Authentication error»/500.

---

### Phase 8 — Тесты, CI и приёмка

- [ ] **8.1. Unit-тесты.**
  - `UserProvisioning`: все статусы, duplicate, null claims, idempotency.
  - `ExternalIdentity` mapping: `sub`, username, email, issuer.
  - Identity separation: collision-тест (см. 3.7).
  - OIDC: state/nonce, PKCE, scopes, claims, userinfo JSON/JWT, errors, подпись ID token (`alg=none`/неверная подпись).
  - Path binding принимает `UserId`/system id; display-имена — только через `UserSearch`, не через path (3.2/3.5).
  - Account actions: `close`/`kid` работают без пароля после SSO; `delete`/`passwd`/`email`/`username`/`twoFactor` недоступны.

- [ ] **8.2. Integration-тесты.**
  - Mock IdP: discovery, token, userinfo, JWKS.
  - Logout/backchannel: подпись, audience, replay.
  - Route tests: локальные auth-эндпоинты → 404.
  - WS handshake: `sessionId` TTL/`enabled`/`identity_status`; kick по `CloseAccount`.
  - Global ban: OAuth API с personal token → отказ; `identity_status` проверяется в `OAuthServer.auth`.

- [ ] **8.3. Команды.**
  - `sbt compile`
  - `sbt "oidc/test"`
  - `sbt "security/test"` (если затронут security)
  - профильные тесты изменённых модулей (`user`, `team`, `msg`, `tournament`).
  - `arena`: сборка/тесты клиента (команда — по package.json).
  - `line-ws`: `sbt test`/`sbt compile` в его репозитории.
  - При наличии — `sbt test`.

- [ ] **8.4. Ручные проверки.**
  - Вход нового пользователя.
  - Повторный вход.
  - Disabled/foreverClosed/deleted.
  - `close`/`kid` без пароля после SSO.
  - Logout → повторный вход.
  - Backchannel logout.
  - Бан при открытом WS → сокет закрывается, а не только новые handshake.
  - Параллельные вкладки.
  - Мобильный/игровой клиент (для будущего flow — отдельно).

**Критерий выхода Phase 8:** все тесты зелёные; негативные сценарии покрыты; нет регрессий в затронутых модулях.

---

### Phase 9 — Out of scope: YandexGame / VkGame / FridayGame (D12 hybrid)

Отдельная задача после Phase 1–8. Фактическое состояние на момент планирования:

- клиент `arena/src` — Capacitor/Yandex Games, один код для first-party и платформ;
- логин: `POST /auth/signature` с `{ provider: 'yandex-game', signature }`, ожидает `Session` с `sessionId`;
- `/auth/signature` в `line` пока не реализован;
- клиент хранит `sessionId` в storage, шлёт его заголовком в HTTP и query-параметром в WS;
- WS (`line-ws`) читает `sessionId` из cookie `live2` или query и проверяет только `security._id + up=true`;
- OAuth/Bearer в клиенте и WS отсутствует.

Архитектурные требования:

- IdP — единственный брокер: signature → IdP → канонический `sub`.
- Lila не принимает platform-id как `_id`; используется тот же `UserProvisioning` и проверки банов.
- Клиент не получает долгоживущий SSO-токен; только одноразовый code/короткоживущий assertion, audience-bound к lila.
- **Hybrid:** существующий клиент/WS остаются на `sessionId`; OAuth Bearer — для API/server-to-server и новых интеграций. Полный OAuth-rewrite клиента/WS не делаем.
- `sessionId` усиливается (Phase 4.6): TTL/expiresAt, закрытие старых сессий при логине, привязка к provider/platform/playerId, проверка `enabled`/`foreverClosed` в WS, отказ от query-логирования.
- Если OAuth понадобится для WS — subprotocol/first-message + fallback на `sessionId`; query-параметр для токена не использовать.
- Platform nickname не использовать как lila username.
- Revocation/короткий TTL для игровых токенов; backchannel logout для web-сессий.
- `/auth/signature` реализуется как адаптер к IdP и возвращает тот же `Session`-контракт (с `sessionId`), чтобы не переписывать клиент.
- `/auth/signature` — `AnonBody`/JSON endpoint с CSRF-exemption; rate limit по IP/playerId; allowlist `provider` (`yandex-game`, `vk-game`, `friday-game`); ограничение размера body; signature не логируется и не сохраняется; lila не доверяет полям из signature — только ответу IdP.
- Клиент ожидает `Session | LobbyData`; при отказе — понятная ошибка, не `ipban`-заглушка без причины.
- Logout на платформах: если у Yandex/VK/Friday нет SSO-logout, после `/logout` клиент может снова автоматически войти через SDK. Зафиксировать продуктовое поведение (гасить платформенную сессию, показывать экран выбора аккаунта или принять auto-login).
- `web:mobile` OAuth-токены требуют HMAC-подписи `oauth.mobile.secret` (`OAuthServer.scala:86-96`), но `/api/token` возвращает только `token.plain`; в этом репозитории не видно, кто подписывает. Для platform/API-flow определить выдачу/подпись или использовать отдельный scope без этой проверки.
- Cross-repo: `arena/src` (session/socket/http) и `line-ws` (Auth/Mongo/LilaHandler) меняются синхронно с lila; при активном WS — kick через `CloseAccount`; при TTL — periodic check.

---

## 5. Трассировка находок → шаги

| Находка | Шаги |
|---|---|
| OIDC-callback выдаёт сессию disabled/foreverClosed пользователю | 1.0, 2.4 |
| Локальный стек аутентификации открыт | 0.1, 1.1–1.6 |
| Email-confirm signup выдаёт сессию; `mustConfirmEmail` уводит OIDC-вход в локальный флоу | 0.1, 1.1 |
| Локальная смена пароля/email/username, локальное удаление | 0.1, 1.1, 3.6 |
| Без `jws_alg` используется plain-валидатор ID token | 1.7 |
| Personal OAuth-токены/appeal-сессии переживают бан | 2.5, 2.7 |
| Замена `historicalRegex` ломает `UserStr.read`/route binding и системные id | 0.2, 3.5 |
| `UserSearch` только ASCII, обрезка на пробеле | 3.2 |
| Нет PKCE | 5.5 |
| `create2` требует email/пароль, `None` в гонке → 500 | 2.2 |
| Reopen через email-token (локальный вход) | 1.4, 2.4 |
| Локальная 2FA обходится OIDC-входом | 1.6 |
| `close`/`kid` требуют пароль и ломаются после отключения локального входа | 1.1, 3.5 |
| Нет TTL у `sessionId`, старые сессии живут вечно | 4.6 |
| WS не проверяет `enabled`/TTL, `sessionId` уходит в query | 4.6 |
| Активные WS переживают бан/удаление | 2.5, 2.5a, 4.6 |
| Global ban не проверяется в OAuthServer | 2.5, 8.2 |
| `sessionId`: cookie перебивает header | 4.6 |
| Клиент `arena` использует username как ключ/маршрут | 3.8 |
| `/auth/signature` есть в клиенте, но не в бэкенде | Phase 9 |
| Обход/некорректная обработка `enabled`/`foreverClosed` | 2.2–2.4 |
| Удалённый/анонимизированный аккаунт → 500 | 2.2, 2.4, 2.5a, 7.5 |
| Глобальный бан не доходит до lila | 2.5, 4.5 |
| `sub` → `_id` без issuer/immutability | 2.1, 2.6 |
| `id != username`, остатки `name.id` | 3.1–3.7 |
| `historicalRegex` = правило UserId | 3.5 |
| Team permissions / addLeader wrong target | 3.3 |
| Mod/Report/PM/Study/Clas/Plan wrong target | 3.3 |
| Tournament Duel использует `name.id` | 3.4 |
| `setUsername` fire-and-forget, нет `len`/cache | 3.6 |
| Logout не завершает SSO-сессию | 4.2 |
| Нет backchannel logout | 4.3–4.5 |
| Scopes `address`/`profile_ex` | 5.1 |
| `response_mode`/лишний POST-роут | 5.3 |
| Null claims / JWT userinfo | 5.2 |
| Ошибки OIDC → 500 | 5.4 |
| state/nonce в общем flash | 5.5 |
| Нет rate limit на callback | 5.6 |
| Metadata/JWKS не кэшируются, blocking I/O | 6.1–6.4 |
| Email null/duplicate/verified | 2.2, 7.1 |
| Referrer потерян | 7.2 |
| Диагностика metadata/конфига | 7.3–7.4 |

---

## 6. Rollout и rollback

- **Dev:** включить `ssoOnly`, прогнать все тесты; проверить, что локальные входы недоступны.
- **Staging:** mock IdP + реальный IdP; проверить logout/backchannel, баны, deleted, duplicate email.
- **Prod:** поэтапно:
  0. хотфикс статуса в `oidcCallback` (1.0) и обязательный `jws_alg` (1.7) — немедленно, независимо от флага;
  1. guard локальных входов (флаг);
  2. provisioning/status + claims (5.2: null-claims, `sub` ID token = userinfo) + ADT ошибок — без 5.2 provisioning ненадёжен;
  3. identity separation;
  4. logout/backchannel;
  5. scopes/state/PKCE/performance.
- **Rollback:** флаг `ssoOnly` можно временно выключить только как аварийную меру; при этом локальные входы снова станут доступны, поэтому rollback должен сопровождаться блокировкой на прокси.
- **Мониторинг:** success/failure login, OIDC error codes, ban denials, duplicate email, backchannel logout, latency to IdP.

---

## 7. Definition of Done

- [ ] Ни один локальный auth-эндпоинт не создаёт сессию/credential (включая email-confirm signup, смену пароля/email/username).
- [ ] `close`/`kid` работают без пароля после SSO; `delete`/`passwd`/`email`/`username`/`twoFactor` недоступны.
- [ ] SSO-вход не выдаёт сессию disabled/foreverClosed/deleted/глобально забаненному пользователю.
- [ ] Подпись ID token проверяется всегда; `jws_alg` обязателен; PKCE S256 включён.
- [ ] Бан/удаление отзывает сессии, appeal-сессии, OAuth-токены и закрывает активные WS.
- [ ] `OAuthServer.auth` и `line-ws/Auth` проверяют `identity_status`/`enabled`/`expiresAt`, а не только `enabled`/`up`.
- [ ] `UserStr.read`/route binding принимают только `UserId` (numeric + системные id); display-username в URL не резолвится.
- [ ] Локальная 2FA отключена: включить её нельзя, `totpSecret` не влияет на вход, MFA — на IdP.
- [ ] Все входы (OIDC, будущий game-flow) идут через единый `UserProvisioning`.
- [ ] `enabled`, `foreverClosed`, tombstone, global ban обрабатываются до выдачи сессии.
- [ ] `UserId` — единственный идентификатор; `UserName` — только отображение.
- [ ] Collision-тест (`username` одного = `_id` другого) проходит для PM/report/team/study/duel.
- [ ] Logout завершает SSO-сессию; backchannel logout работает.
- [ ] `sessionId` имеет TTL, привязан к платформе, старые сессии закрываются; `line-ws` проверяет TTL/`enabled`; `sessionId` не уходит в query-логи; header/cookie precedence зафиксирован.
- [ ] Клиент `arena/src` и `line-ws` обновлены синхронно: username не используется как ключ, WS принимает/отзывает сессии по новым правилам.
- [ ] D12 hybrid зафиксирован: клиент/WS на `sessionId`, OAuth Bearer — для API/server-to-server; `/auth/signature` реализован как адаптер к IdP.
- [ ] Scopes соответствуют IdP; используется только `response_mode=query`, `POST /signin-oidc` отсутствует.
- [ ] Негативные OIDC-сценарии дают управляемый ответ, не 500.
- [ ] Metadata/JWKS кэшируются; таймауты конечны; нет blocking I/O на request dispatcher.
- [ ] Тесты и CI зелёные; документация по решениям D1–D12 обновлена.

---

## 8. Риски

| Риск | Вероятность | Влияние | Митигация |
|---|---|---|---|
| Отключение локального входа ломает старых клиентов/API | Средняя | Высокое | Инвентаризация 0.1, поэтапный rollout, связь с клиентами |
| `preferred_username` не уникален/меняется, содержит пробелы | Средняя | Высокое | D2/D11 (подтверждены): non-unique index, только display, адресация по `UserId`, поиск с выбором; username не используется в URL/mentions |
| Нарушение SSO-контракта immutable `sub` | Низкая | Критическое | D3 (подтверждён): мониторинг аномалий, tombstone, аудит |
| Duplicate/absent email в 350k SSO | Высокая | Низкое | D5 (подтверждён): дубли разрешены, email не identity, unique index убран, email-флоу выключены |
| Глобальный бан не доходит до lila | Средняя | Высокое | D4: signed events + projection + claim + backchannel logout + reconciliation |
| Identity refactor затронет много модулей | Высокая | Среднее | Компилятор-driven подход 3.1, тесты 3.7, поэтапные PR |
| Backchannel требует изменений на IdP | Средняя | Среднее | IdP под контролем; согласовать endpoint/audience |
| `sessionId` без TTL / старые сессии / утечка query в WS | Высокая | Высокое | Phase 4.6: TTL, закрытие старых, привязка, WS `enabled`, subprotocol/cookie |
| `close`/`kid` ломаются из-за парольных форм | Высокая | Среднее | 1.1: passwordless SSO-подтверждение, переписать `myUsernameField` |
| Активные WS переживают бан/удаление | Высокая | Высокое | 2.5/4.6: `CloseAccount`/`disconnectUser` + periodic check |
| Global ban не проверяется в OAuth API | Средняя | Высокое | 2.5/8.2: `identity_status` в `OAuthServer.auth` |
| Клиент `arena` использует username как ключ | Высокая | Высокое | 3.8: аудит/переход на `LightUser.id`/`Session.id` |
| `/auth/signature` в клиенте, но не в бэкенде | Высокая | Среднее | Phase 9: адаптер к IdP с тем же `Session`-контрактом + CSRF/rate limit/allowlist |
| Потеря производительности при blocking I/O | Средняя | Высокое | Phase 6, нагрузочный тест |
| Смена regex username ломает роутинг `/@/:id` и системные аккаунты | Высокая | Высокое | 3.5: два раздельных правила, whitelist системных id, route-тесты |
| Изменения в `line-ws` рассинхронизированы с `line` | Средняя | Среднее | Отдельные PR в `line-ws`, совместимость полей `security` (TTL/`expiresAt`) с обеих сторон, общий тест-сценарий |

---

## 9. Статус решений

Все решения **D1–D12 закрыты** (см. журнал в разделе 2). Блокирующих вопросов нет.

Можно переходить к исполнению Phase 0–8 в порядке приоритета. Phase 9 (signature-flow) остаётся отдельной задачей и использует зафиксированный D12 hybrid.
