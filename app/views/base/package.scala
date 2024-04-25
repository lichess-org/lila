package views.html.base

import lila.app.templating.Environment.{ *, given }

val bits         = lila.web.views.bits()
val atom         = lila.web.views.atom(netBaseUrl)
lazy val captcha = lila.web.views.captcha(helpers)
