package views

import lila.app.templating.Environment.*
import lila.game.GameExt.playerBlurPercent

lazy val chat = lila.chat.ChatUi(helpers)

lazy val gathering = lila.gathering.ui.GatheringUi(helpers)(env.web.settings.prizeTournamentMakers.get)

lazy val learn = lila.web.views.LearnUi(helpers)

lazy val coordinate = lila.coordinate.ui.CoordinateUi(helpers)

val irwin = lila.irwin.IrwinUi(helpers)(
  menu = views.mod.ui.menu,
  playerBlurPercent = pov => pov.game.playerBlurPercent(pov.color)
)

object oAuth:
  val token     = lila.oauth.ui.TokenUi(helpers)(views.account.ui.AccountPage)
  val authorize = lila.oauth.ui.AuthorizeUi(helpers)(lightUserFallback)

lazy val plan      = lila.plan.ui.PlanUi(helpers)(netConfig.email)
lazy val planPages = lila.plan.ui.PlanPages(helpers)(lila.fishnet.FishnetLimiter.maxPerDay)
