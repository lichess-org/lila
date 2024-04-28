package views.base

import lila.app.templating.Environment.{ *, given }

val bits         = lila.web.views.bits()
lazy val captcha = lila.web.views.captcha(helpers)
