package views.html.mod

import lila.app.templating.Environment.{ *, given }
import lila.mod.ui.*

lazy val ui = ModUi(helpers)

lazy val userTable = ModUserTableUi(helpers, ui)

lazy val user = ModUserUi(helpers, ui)
