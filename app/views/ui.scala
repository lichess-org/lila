package views

import lila.app.templating.Environment.*

lazy val chat = lila.chat.ChatUi(helpers)

lazy val gathering = lila.gathering.ui.GatheringUi(helpers)(env.web.settings.prizeTournamentMakers.get)

lazy val learn = lila.web.views.LearnUi(helpers)

object oAuth:
  val token     = lila.oauth.ui.TokenUi(helpers)(views.account.ui.AccountPage)
  val authorize = lila.oauth.ui.AuthorizeUi(helpers)(lightUserFallback)
