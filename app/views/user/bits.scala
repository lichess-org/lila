package views.html.user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.PerfType
import lila.user.User

import controllers.routes

object bits {

  def communityMenu(active: String)(implicit ctx: Context) =
    st.nav(cls := "page-menu__menu subnav")(
      a(cls := active.active("leaderboard"), href := routes.User.list)(trans.leaderboard()),
      a(cls := active.active("ratings"), href := routes.Stat.ratingDistribution("blitz"))(trans.ratingStats()),
      a(cls := active.active("tournament"), href := routes.Tournament.leaderboard)(trans.tournamentWinners()),
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

  def perfTrophies(u: User, rankMap: lila.rating.UserRankMap)(implicit ctx: Context) = !u.lame ??
    rankMap.toList.sortBy(_._2).collect {
      case (perf, rank) if rank == 1 =>
        span(cls := "trophy perf top1", title := s"${perf.name} Champion!")(
          img(src := staticUrl("images/trophy/Big-Gold-Cup.png"))
        )
      case (perf, rank) if rank <= 10 =>
        span(cls := "trophy perf top10", title := s"${perf.name} Top 10!")(
          img(src := staticUrl("images/trophy/Big-Silver-Cup.png"))
        )
      case (perf, rank) if rank <= 50 =>
        span(cls := "trophy perf top50", title := s"${perf.name} Top 50 player!")(
          img(src := staticUrl("images/trophy/Fancy-Gold.png"))
        )
      case (perf, rank) if rank <= 100 =>
        span(cls := "trophy perf", title := s"${perf.name} Top 100 player!")(
          img(src := staticUrl("images/trophy/Gold-Cup.png"))
        )
    }
}
