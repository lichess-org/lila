package views.mod

import play.api.data.Form
import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }
import lila.mod.ui.*
import lila.mod.ModActivity
import lila.core.perf.UserWithPerfs

lazy val ui = ModUi(helpers)(() => env.chat.panic.enabled)

lazy val userTable = ModUserTableUi(helpers, ui)

lazy val user = ModUserUi(helpers, ui)

def log(logs: List[lila.mod.Modlog])(using PageContext) =
  views.base.layout(title = "My logs", moreCss = cssTag("mod.misc"))(ui.myLogs(logs))

def chatPanic(state: Option[Instant])(using PageContext) =
  views.base.layout(title = "Chat Panic", moreCss = cssTag("mod.misc"))(ui.chatPanic(state))

def presets(group: String, form: Form[?])(using PageContext) =
  views.base.layout(
    title = s"$group presets",
    moreCss = frag(cssTag("mod.misc"), cssTag("form3"))
  )(ui.presets(group, form))

def permissions(u: User)(using ctx: PageContext, me: Me) =
  views.base.layout(
    title = s"${u.username} permissions",
    moreCss = frag(cssTag("mod.permission"), cssTag("form3"))
  )(ui.permissions(u, lila.security.Permission.categorized))

def activity(p: ModActivity.Result)(using PageContext) =
  views.base.layout(
    title = "Moderation activity",
    moreCss = cssTag("mod.activity"),
    pageModule = PageModule("mod.activity", Json.obj("op" -> "activity", "data" -> ModActivity.json(p))).some
  )(ui.activity(p, ui.menu("activity")))

def queueStats(p: lila.mod.ModQueueStats.Result)(using PageContext) =
  views.base.layout(
    title = "Queues stats",
    moreCss = cssTag("mod.activity"),
    pageModule = PageModule("mod.activity", Json.obj("op" -> "queues", "data" -> p.json)).some
  )(ui.queueStats(p, ui.menu("queues")))

def emailConfirm(query: String, user: Option[UserWithPerfs], email: Option[EmailAddress])(using
    ctx: PageContext
) =
  views.base.layout(
    title = "Email confirmation",
    moreCss = cssTag("mod.misc"),
    moreJs = embedJsUnsafeLoadThen(ui.emailConfirmJs)
  )(ui.emailConfirm(query, user, email, views.mod.ui.menu("email")))
