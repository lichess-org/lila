package views.html.user

import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User

import controllers.routes

object bits:

  def communityMenu(active: String)(using Context) =
    views.html.site.bits.pageMenuSubnav(
      a(cls := active.active("leaderboard"), href := routes.User.list)(trans.leaderboard()),
      a(
        cls  := active.active("ratings"),
        href := routes.User.ratingDistribution("blitz")
      )(
        trans.ratingStats()
      ),
      a(cls := active.active("tournament"), href := routes.Tournament.leaderboard)(
        trans.tournamentWinners()
      ),
      a(cls := active.active("shield"), href := routes.Tournament.shields)("Shields"),
      a(cls := active.active("bots"), href := routes.PlayApi.botOnline)("Online bots")
    )

  def miniClosed(u: User)(using Context) =
    frag(
      div(cls := "title")(userLink(u, withPowerTip = false)),
      div(style := "padding: 20px 8px; text-align: center")(trans.settings.thisAccountIsClosed())
    )

  def signalBars(v: Int) = raw:
    val bars = (1 to 4).map { b =>
      s"""<i${if v < b then " class=\"off\"" else ""}></i>"""
    } mkString ""
    val title = v match
      case 1 => "Poor connection"
      case 2 => "Decent connection"
      case 3 => "Good connection"
      case _ => "Excellent connection"
    s"""<signal title="$title" class="q$v">$bars</signal>"""

  def perfTrophies(u: User, rankMap: lila.rating.UserRankMap)(using Lang) =
    !u.lame so
      rankMap.toList.sortBy(_._2).collect {
        case (perf, rank) if rank == 1 =>
          span(cls := "trophy perf top1", title := s"${perf.trans} Champion!")(
            img(src := assetUrl("images/trophy/Big-Gold-Cup.png"))
          )
        case (perf, rank) if rank <= 10 =>
          span(cls := "trophy perf top10", title := s"${perf.trans} Top 10!")(
            img(src := assetUrl("images/trophy/Big-Silver-Cup.png"))
          )
        case (perf, rank) if rank <= 50 =>
          span(cls := "trophy perf top50", title := s"${perf.trans} Top 50 player!")(
            img(src := assetUrl("images/trophy/Fancy-Gold.png"))
          )
        case (perf, rank) if rank <= 100 =>
          span(cls := "trophy perf", title := s"${perf.trans} Top 100 player!")(
            img(src := assetUrl("images/trophy/Gold-Cup.png"))
          )
      }
