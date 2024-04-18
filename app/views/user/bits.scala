package views.html.user

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }

import lila.rating.PerfType

object bits:

  lazy val ui = lila.user.ui.UserUiBits()(assetUrl)
  export ui.*

  def communityMenu(active: String)(using Context) =
    views.html.base.bits.pageMenuSubnav(
      a(cls := active.active("leaderboard"), href := routes.User.list)(trans.site.leaderboard()),
      a(
        cls  := active.active("ratings"),
        href := routes.User.ratingDistribution("blitz")
      )(
        trans.site.ratingStats()
      ),
      a(cls := active.active("tournament"), href := routes.Tournament.leaderboard)(
        trans.arena.tournamentWinners()
      ),
      a(cls := active.active("shield"), href := routes.Tournament.shields)(
        trans.arena.tournamentShields()
      ),
      a(cls := active.active("bots"), href := routes.PlayApi.botOnline)(
        trans.site.onlineBots()
      )
    )

  def miniClosed(u: User)(using Context) =
    frag(
      div(cls := "title")(userLink(u, withPowerTip = false)),
      div(style := "padding: 20px 8px; text-align: center")(trans.settings.thisAccountIsClosed())
    )
