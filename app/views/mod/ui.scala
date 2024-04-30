package views.mod

import lila.app.templating.Environment.{ *, given }
import lila.mod.ui.*

lazy val ui        = ModUi(helpers)(() => env.chat.panic.enabled)
lazy val userTable = ModUserTableUi(helpers, ui)
lazy val user      = ModUserUi(helpers, ui)

def permissions(u: User)(using Context, Me) =
  ui.permissions(u, lila.security.Permission.categorized)

export ui.emailConfirm
