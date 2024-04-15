package views.html.base

import lila.app.templating.Environment.{ *, given }

val bits = lila.web.views.bits(controllers.routes.Main.externalLink)
val atom = lila.web.views.atom(netBaseUrl)
