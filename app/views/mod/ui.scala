package views.html.mod

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.mod.ui.*

lazy val ui = ModUi(helpers)(() => env.chat.panic.enabled)

lazy val userTable = ModUserTableUi(helpers, ui)

lazy val user = ModUserUi(helpers, ui)

def log(logs: List[lila.mod.Modlog])(using PageContext) =
  views.html.base.layout(title = "My logs", moreCss = cssTag("mod.misc"))(ui.myLogs(logs))

def chatPanic(state: Option[Instant])(using PageContext) =
  views.html.base.layout(title = "Chat Panic", moreCss = cssTag("mod.misc"))(ui.chatPanic(state))

def presets(group: String, form: Form[?])(using PageContext) =
  views.html.base.layout(
    title = s"$group presets",
    moreCss = frag(cssTag("mod.misc"), cssTag("form3"))
  )(ui.presets(group, form))

def permissions(u: User)(using ctx: PageContext, me: Me) =
  views.html.base.layout(
    title = s"${u.username} permissions",
    moreCss = frag(cssTag("mod.permission"), cssTag("form3"))
  )(ui.permissions(u, lila.security.Permission.categorized))

def impersonate(user: User)(using Translate) =
  div(id := "impersonate")(
    div(cls := "meat")("You are impersonating ", userLink(user, withOnline = false)),
    div(cls := "actions")(
      postForm(action := routes.Mod.impersonate("-"))(submitButton(cls := "button button-empty")("Quit"))
    )
  )
