package views.html
package user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object bits {

  def communityMenu(active: String)(implicit ctx: Context) =
    st.nav(cls := "page-menu__menu subnav")(
      a(cls := active.active("leaderboard"), href := routes.User.list)(trans.leaderboard.frag()),
      a(cls := active.active("ratings"), href := routes.Stat.ratingDistribution("blitz"))(trans.ratingStats.frag()),
      a(cls := active.active("tournament"), href := routes.Tournament.leaderboard)(trans.tournamentWinners.frag()),
      a(cls := active.active("shield"), href := routes.Tournament.shields)("Shields")
    )

  def miniClosed(u: User)(implicit ctx: Context) = frag(
    div(cls := "title")(userLink(u, withPowerTip = false)),
    div(style := "padding: 20px 8px; text-align: center")(trans.thisAccountIsClosed())
  )

  def signalBars(v: Int) = raw {
    val bars = (1 to 4).map { b =>
      s"""<i${if (v < b) " class=\"off\"" else ""}></i>"""
    } mkString ""
    val title = v match {
      case 1 => "Poor connection"
      case 2 => "Decent connection"
      case 3 => "Good connection"
      case _ => "Excellent connection"
    }
    s"""<signal title="$title" class="q$v">$bars</signal>"""
  }
}
