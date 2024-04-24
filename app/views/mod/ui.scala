package views.html.mod

import lila.app.templating.Environment.{ *, given }
import lila.mod.ui.*

lazy val ui = ModUi(kitchenSink)

lazy val userTable = ModUserTableUi(kitchenSink, ui)

lazy val user = ModUserUi(kitchenSink, ui, lightUserSync)
