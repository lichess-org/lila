package views.html
package user

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.rating.PerfType
import lila.user.User
import lila.common.licon

import controllers.routes

object list:

  def apply(
      tourneyWinners: List[lila.tournament.Winner],
      online: List[User.WithPerfs],
      leaderboards: lila.user.UserPerfs.Leaderboards,
      nbAllTime: List[User.LightCount]
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = trans.players.txt(),
      moreCss = cssTag("user.list"),
      wrapClass = "full-screen-force",
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Chess players and leaderboards",
          url = s"$netBaseUrl${routes.User.list.url}",
          description =
            "Best chess players in bullet, blitz, rapid, classical, Chess960 and more chess variants"
        )
        .some
    ) {
      main(cls := "page-menu")(
        bits.communityMenu("leaderboard"),
        div(cls := "community page-menu__content box box-pad")(
          st.section(cls := "community__online")(
            h2(trans.onlinePlayers()),
            ol(cls := "user-top"):
              online.map: u =>
                li(
                  userLink(u),
                  ctx.pref.showRatings option showBestPerf(u.perfs)
                )
          ),
          div(cls := "community__leaders")(
            h2(trans.leaderboard()),
            div(cls := "leaderboards")(
              userTopPerf(leaderboards.bullet, PerfType.Bullet),
              userTopPerf(leaderboards.blitz, PerfType.Blitz),
              userTopPerf(leaderboards.rapid, PerfType.Rapid),
              userTopPerf(leaderboards.classical, PerfType.Classical),
              userTopPerf(leaderboards.ultraBullet, PerfType.UltraBullet),
              userTopActive(nbAllTime, trans.activePlayers(), icon = licon.Swords.some),
              tournamentWinners(tourneyWinners),
              userTopPerf(leaderboards.crazyhouse, PerfType.Crazyhouse),
              userTopPerf(leaderboards.chess960, PerfType.Chess960),
              userTopPerf(leaderboards.antichess, PerfType.Antichess),
              userTopPerf(leaderboards.atomic, PerfType.Atomic),
              userTopPerf(leaderboards.threeCheck, PerfType.ThreeCheck),
              userTopPerf(leaderboards.kingOfTheHill, PerfType.KingOfTheHill),
              userTopPerf(leaderboards.horde, PerfType.Horde),
              userTopPerf(leaderboards.racingKings, PerfType.RacingKings)
            )
          )
        )
      )
    }

  private def tournamentWinners(winners: List[lila.tournament.Winner])(using Context) =
    st.section(cls := "user-top")(
      h2(cls := "text", dataIcon := licon.Trophy)(
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

  private def userTopPerf(users: List[User.LightPerf], perfType: PerfType)(using ctx: Context) =
    st.section(cls := "user-top")(
      h2(cls := "text", dataIcon := perfType.icon)(
        a(href := routes.User.topNb(200, perfType.key))(perfType.trans)
      ),
      ol(users map { l =>
        li(
          lightUserLink(l.user),
          ctx.pref.showRatings option l.rating
        )
      })
    )

  private def userTopActive(users: List[User.LightCount], hTitle: Frag, icon: Option[licon.Icon])(using
      Context
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
