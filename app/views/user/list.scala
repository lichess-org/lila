package views.html
package user

import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.PerfType
import lila.user.User

import controllers.routes

object list {

  def apply(
      tourneyWinners: List[lila.tournament.Winner],
      online: List[User],
      leaderboards: lila.user.Perfs.Leaderboards,
      nbAllTime: List[User.LightCount]
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.players.txt(),
      moreCss = cssTag("user.list"),
      wrapClass = "full-screen-force",
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${trans.players.txt()} - ${trans.leaderboards.txt()}",
          url = s"$netBaseUrl${routes.User.list.url}",
          description = trans.bestPlayersInVariousCategories.txt()
        )
        .some
      // todo later - withHrefLangs = lila.i18n.LangList.All.some
    ) {
      main(cls := "page-menu")(
        bits.communityMenu("leaderboard"),
        div(cls := "community page-menu__content box box-pad")(
          st.section(cls := "community__online")(
            h2(trans.onlinePlayers()),
            ol(cls := "user-top")(online map { u =>
              li(
                userLink(u),
                showBestPerf(u)
              )
            })
          ),
          div(cls := "community__leaders")(
            h2(trans.leaderboard()),
            div(cls := "leaderboards")(
              userTopPerf(leaderboards.bullet, PerfType.Bullet),
              userTopPerf(leaderboards.blitz, PerfType.Blitz),
              userTopPerf(leaderboards.rapid, PerfType.Rapid),
              userTopPerf(leaderboards.classical, PerfType.Classical),
              userTopPerf(leaderboards.ultraBullet, PerfType.UltraBullet),
              userTopPerf(leaderboards.correspondence, PerfType.Correspondence),
              userTopActive(nbAllTime, trans.activePlayers(), icon = 'U'.some),
              tournamentWinners(tourneyWinners),
              userTopPerf(leaderboards.minishogi, PerfType.Minishogi),
              userTopPerf(leaderboards.chushogi, PerfType.Chushogi),
              userTopPerf(leaderboards.annanshogi, PerfType.Annanshogi),
              userTopPerf(leaderboards.kyotoshogi, PerfType.Kyotoshogi),
              userTopPerf(leaderboards.checkshogi, PerfType.Checkshogi)
            )
          )
        )
      )
    }

  private def tournamentWinners(winners: List[lila.tournament.Winner])(implicit ctx: Context) =
    st.section(cls := "user-top")(
      h2(cls := "text", dataIcon := "g")(
        a(href := routes.Tournament.leaderboard)(trans.tournament())
      ),
      ol(winners take 10 map { w =>
        li(
          userIdLink(w.userId.some),
          a(title := w.tourName, href := routes.Tournament.show(w.tourId))(
            scheduledTournamentNameShortHtml(w.tourName)
          )
        )
      })
    )

  private def userTopPerf(users: List[User.LightPerf], perfType: PerfType)(implicit lang: Lang) =
    st.section(cls := "user-top")(
      h2(cls := "text", dataIcon := perfType.iconChar)(
        a(href := routes.User.topNb(200, perfType.key))(perfType.trans)
      ),
      ol(users map { l =>
        li(
          lightUserLink(l.user),
          l.rating
        )
      })
    )

  private def userTopActive(users: List[User.LightCount], hTitle: Frag, icon: Option[Char])(implicit
      ctx: Context
  ) =
    st.section(cls := "user-top")(
      h2(cls := "text", dataIcon := icon.map(_.toString))(hTitle),
      ol(users map { u =>
        li(
          lightUserLink(u.user),
          span(title := trans.gamesPlayed.txt())(s"#${u.count.localize}")
        )
      })
    )
}
