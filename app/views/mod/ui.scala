package views.mod

import lila.app.UiEnv.*
import lila.mod.ui.*

lazy val ui         = ModUi(helpers)(() => env.chat.panic.enabled)
lazy val userTable  = ModUserTableUi(helpers, ui)
lazy val user       = ModUserUi(helpers, ui)
lazy val gamify     = GamifyUi(helpers, ui)
lazy val publicChat = PublicChatUi(helpers, ui)(lila.shutup.Analyser.highlightBad)

def permissions(u: User)(using Context, Me) =
  ui.permissions(u, lila.security.Permission.categorized)
