package views.mod

import lila.app.templating.Environment.{ *, given }
import lila.mod.ui.*
import lila.core.perf.UserWithPerfs

lazy val ui        = ModUi(helpers)(() => env.chat.panic.enabled)
lazy val userTable = ModUserTableUi(helpers, ui)
lazy val user      = ModUserUi(helpers, ui)

def permissions(u: User)(using PageContext, Me) =
  ui.permissions(u, lila.security.Permission.categorized)

def emailConfirm(query: String, user: Option[UserWithPerfs], email: Option[EmailAddress])(using PageContext) =
  ui.emailConfirm(query, user, email)
