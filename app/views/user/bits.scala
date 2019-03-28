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

  def perfTrophies(u: User, rankMap: Option[lila.rating.UserRankMap])(implicit ctx: Context) =
    rankMap.ifFalse(u.lame).map { ranks =>
      ranks.toList.sortBy(_._2).collect {
        case (perf, rank) if rank == 1 =>
          span(cls := "trophy perf", title := s"${PerfType.name(perf)} Champion!")(
            img(src := staticUrl("images/trophy/Big-Gold-Cup.png"), height := 80)
          )
        case (perf, rank) if rank <= 10 =>
          span(cls := "trophy perf", title := s"${PerfType.name(perf)} Top 10!")(
            img(src := staticUrl("images/trophy/Big-Silver-Cup.png"), height := 80)
          )
        case (perf, rank) if rank <= 50 =>
          span(cls := "trophy perf", title := s"${PerfType.name(perf)} Top 50 player!")(
            img(src := staticUrl("images/trophy/Fancy-Gold.png"), height := 80)
          )
        case (perf, rank) if rank <= 100 =>
          span(cls := "trophy perf", title := s"${PerfType.name(perf)} Top 100 player!")(
            img(src := staticUrl("images/trophy/Gold-Cup.png"), height := 80)
          )
      }
    }
}
