package views.html.base

import lila.app.templating.Environment.{ *, given }

val bits = lila.web.views.bits(routes.Main.externalLink)
val atom = lila.web.views.atom(netBaseUrl)
lazy val captcha = lila.web.views.captcha(form3, i18nHelper, netBaseUrl)(
  routeRoundWatcher = routes.Round.watcher,
  routeCaptchaCheck = routes.Main.captchaCheck
)
