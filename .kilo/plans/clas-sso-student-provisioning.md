# Clas: создание учеников через SSO

Статус: отложено, реализовать до включения классов/учеников в проде.
Связанный план: `.kilo/plans/oidc-sso-hardening-plan.md`.
Дата: 2026-10-20.

## Проблема

`ClasApi.student.create` создаёт **локальный credential-аккаунт**:

- `userRepo.createLocal` + `authenticator.passEnc(password)`;
- пароль генерируется и показывается учителю один раз через flash;
- синтетический email `noreply.class.<clasId>.<username>@lichess.org`;
- `managed=true`, kid mode, welcome-сообщение;
- `studentResetPassword` генерирует новый локальный пароль.

После перехода на SSO удалены `POST /login` и локальный password-login.
Следовательно, созданные так ученики **не могут войти**: пароль есть, маршрута
входа нет. Секция «Create a new Chess-Online account» и «Create multiple
accounts» сейчас создают нерабочие аккаунты.

Файлы:

- `app/controllers/Clas.scala:283` (`studentCreate`), `:304` (`studentManyForm`), `:325` (`studentManyCreate`), `:415` (`studentResetPassword`)
- `modules/clas/src/main/ClasApi.scala:240` (`create`), `:298` (`resetPassword`)
- `modules/clas/src/main/ClasForm.scala:50` (`create`), `:79` (`manyCreate`)
- `modules/clas/src/main/ui/StudentFormUi.scala:29` (`newStudent`)
- `modules/clas/src/main/ui/StudentUi.scala` (reset password)

## Цель

Ученик получает доступ только через SSO IdP. Lila не создаёт и не хранит
локальные пароли учеников. Учитель либо приглашает существующий SSO-аккаунт,
либо создаёт ученика в IdP через серверную интеграцию.

## Варианты

### A. Invite-only (минимальный, рекомендуемый для запуска)

- Убрать «Create a new account» и «Create multiple accounts».
- Оставить только приглашение существующего аккаунта.
- Ученик (или родитель) заранее регистрируется в passport/IdP, затем учитель
  приглашает его по `userId` (autocomplete возвращает ID, не display-name).
- Плюсы: нет работы с паролями и PII детей, минимум кода.
- Минусы: учитель не может создать аккаунт «с нуля»; нужен IdP-аккаунт.

### B. IdP provisioning API (полный)

- Backend lila вызывает защищённый API IdP для создания ученика.
- IdP возвращает `sub`; lila создаёт/связывает аккаунт (`identity_alias`/`sub`).
- Учитель получает не пароль, а ссылку-приглашение/инструкцию для входа через IdP.
- Плюсы: UX «создать ученика» сохраняется.
- Минусы: нужен контракт с IdP, mTLS/HMAC, обработка ошибок,
  согласие родителей/COPPA/GDPR, идемпотентность.

### C. Гибрид

Invite-only сейчас; IdP provisioning — позже, если продукту действительно нужно
создавать учеников без самостоятельной регистрации.

## Рекомендуемый порядок работ

### Этап 1. Заблокировать локальное создание (до включения классов)

1. В `Clas` контроллере добавить guard `env.ssoOnly`:
   - `studentCreate`, `studentManyForm`, `studentManyCreate`,
     `studentResetPassword` → `404`/redirect с понятным сообщением;
   - `ClasApi.create`/`resetPassword` оставить только для не-SSO режима.
2. В `StudentFormUi` скрыть при `ssoOnly`:
   - секцию «Create a new Chess-Online account»;
   - секцию «Create multiple accounts»;
   - кнопку «Reset password» в `StudentUi`.
3. В `ClasForm.student.create`/`manyCreate` не принимать запросы в SSO-режиме
   (defense in depth).
4. Acceptance: при `auth.ssoOnly=true` нет способа создать локальный пароль
   ученика; invite существующего аккаунта работает.

### Этап 2. Приведение invite-флоу к ID-модели

1. Поле приглашения — только `userId` из autocomplete (`userComplete`),
   не display-name.
2. `ClasForm.student.invite`: `historicalField` уже принимает только ID;
   проверить, что UI отправляет ID.
3. `Student.make` для приглашённого: `managed=false`, без пароля.
4. Acceptance: учитель приглашает по ID; ученик входит через SSO; `student.userId`
   совпадает с `sub`/alias.

### Этап 3. IdP provisioning (если нужен «создать с нуля»)

1. Контракт: `POST <idp>/api/clas/students` (mTLS/HMAC), поля:
   `realName`, `username?`, `guardianEmail?`, `clasId`, `teacherSub`.
   Ответ: `sub`, `username`, `status`.
2. Конфиг:
   ```hocon
   clas.provisioning {
     enabled = false
     endpoint = ""
     secret = ""
   }
   ```
3. Сервис `ClasStudentProvisioner`:
   - idempotency key = `clasId + teacherSub + realName` или внешний `requestId`;
   - таймауты/retry, типизированные ошибки, аудит без PII;
   - при успехе: `UserProvisioning`/`identity_alias` создаёт локального user
     с `_id = sub`, без `bpass`.
4. UI: вместо пароля показывать «аккаунт создан в IdP, вход через passport».
5. Acceptance: создание идемпотентно; повторный запрос не плодит пользователей;
   ошибки IdP видны учителю без утечки секретов.

### Этап 4. Данные и миграция

1. Новые managed-ученики: только SSO, `mustConfirmEmail=false`,
   email опционален/из IdP.
2. Legacy managed-аккаунты: не удалять автоматически; для каждого
   `identity_alias(sub → userId)` или invite после регистрации в IdP.
3. Тестовые аккаунты из аудита удалены скриптом
   `bin/mongodb/migrations/2026-10-20-delete-clas-test-accounts.js`
   (dry-run по умолчанию); игры/сообщения не трогаются.
4. `resetPassword` для SSO-учеников не существует; сброс — только в IdP.

### Этап 5. Тесты

- unit: guard `ssoOnly` для `studentCreate`/`manyCreate`/`resetPassword`;
- integration (mock IdP): invite по ID, provisioning success/duplicate/error;
- UI smoke: секции Create/Multi/Reset отсутствуют при `ssoOnly`;
- security: нет пароля в логах/flash/БД для SSO-учеников.

## Открытые вопросы

- Поддерживает ли IdP создание детских аккаунтов без email/телефона?
- Кто владеет username: IdP или lila (display-name)?
- Нужен ли guardian consent и как он фиксируется в IdP?
- Должны ли существующие managed-ученики иметь возможность входа через SSO
  без отдельной регистрации (invite-link)?
- Нужны ли классы в проде вообще; если нет — оставить Этап 1 и не делать 3–5.

## Критерии готовности

- В SSO-режиме невозможно создать локальный пароль ученика.
- Ученик входит только через IdP; `sub` связан с `student.userId` (напрямую или alias).
- Учитель может пригласить существующий SSO-аккаунт по ID.
- Нет паролей/credentials учеников в lila.
- Тесты и документация обновлены; runbook содержит порядок для классов.
