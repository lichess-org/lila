package views.account

import lila.app.templating.Environment.{ *, given }

lazy val ui        = lila.pref.ui.AccountUi(helpers)
lazy val pages     = lila.pref.ui.AccountPages(helpers, ui, flagApi)
lazy val pref      = lila.pref.ui.AccountPref(helpers, prefHelper, ui)
lazy val twoFactor = lila.pref.ui.TwoFactorUi(helpers, ui)
lazy val security  = lila.security.ui.AccountSecurity(helpers)(ui.AccountPage)
