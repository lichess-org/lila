package views.html.user

import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object bits {

  def communityMenu(active: String)(implicit ctx: Context) =
    st.nav(cls := "page-menu__menu subnav")(
      a(cls := active.active("leaderboard"), href := routes.User.list)(trans.leaderboard()),
      a(cls := active.active("ratings"), href := routes.Stat.ratingDistribution("blitz"))(
        trans.ratingStats()
      ),
      a(cls := active.active("tournament"), href := routes.Tournament.leaderboard)(
        trans.tournamentWinners()
      ),
      a(cls := active.active("shield"), href := routes.Tournament.shields)(trans.tournamentShields()),
      a(cls := active.active("bots"), href := routes.PlayApi.botOnline)(trans.onlineBots())
    )

  def miniClosed(u: User)(implicit ctx: Context) =
    frag(
      div(cls := "title")(userLink(u, withPowerTip = false)),
      div(style := "padding: 20px 8px; text-align: center")(trans.settings.thisAccountIsClosed())
    )

  def signalBars(v: Int) =
    raw {
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

  def perfTrophies(u: User, rankMap: lila.rating.UserRankMap)(implicit lang: Lang) =
    !u.lame ??
      rankMap.toList.sortBy(_._2).collect {
        case (perf, rank) if rank == 1 =>
          span(cls  := "trophy perf top1", title := s"${perf.trans} Champion!")(
            img(src := staticUrl("images/trophy/Big-Gold-Cup.png"))
          )
        case (perf, rank) if rank <= 2 =>
          span(cls  := "trophy perf top2", title := s"${perf.trans} Top 2!")(
            img(src := staticUrl("images/trophy/Big-Silver-Cup.png"))
          )
        case (perf, rank) if rank <= 3 =>
          span(cls  := "trophy perf top3", title := s"${perf.trans} Top 3 player!")(
            img(src := staticUrl("images/trophy/Fancy-Gold.png"))
          )
        case (perf, rank) if rank <= 10 =>
          span(cls  := "trophy perf top10", title := s"${perf.trans} Top 10 player!")(
            img(src := staticUrl("images/trophy/Gold-Cup.png"))
          )
      }

  def claimTitle =
    div(cls := "claim-title")(
      h2(dataIcon := "'", cls := "text")("Congratulations for breaking the 2500 rating threshold!"),
      p(
        "To ensure honest players aren't falsely accused of cheating, we request titled players ",
        "to identify themselves.",
        "You can confirm your title and decide to remain anonymous. We will not reveal your identity."
      ),
      p(
        "Please contact us by email at ",
        contactEmailLink,
        ". You can also contact a moderator on lishogi.org."
      ),
      postForm(action := routes.Pref.verifyTitle)(
        button(cls := "button text", dataIcon := "E", name := "v", value := true)("Got it, thanks!"),
        button(cls := "button", name := "v", value := false)("I don't have an official title")
      )
    )
}
