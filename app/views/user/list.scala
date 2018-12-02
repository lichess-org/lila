package views
package html.user

import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._
import lila.i18n.{ I18nKeys => trans }
import lila.rating.PerfType
import lila.user.User

import controllers.routes

object list {

  def apply(
    tourneyWinners: List[lila.tournament.Winner],
    online: List[User],
    leaderboards: lila.user.Perfs.Leaderboards,
    nbDay: List[User.LightCount],
    nbAllTime: List[User.LightCount]
  )(implicit ctx: Context) = layout(
    title = trans.players.txt(),
    side = Some(side(online)),
    openGraph = lila.app.ui.OpenGraph(
      title = "Chess players and leaderboards",
      url = s"$netBaseUrl${routes.User.list.url}",
      description = "Best chess players in bullet, blitz, rapid, classical, Chess960 and more chess variants"
    ).some,
    withInfScroll = false
  ) {
      div(cls := "content_box community")(
        communityTabs("leaderboard"),
        div(cls := "user_lists")(
          userTopPerf(leaderboards.bullet, PerfType.Bullet),
          userTopPerf(leaderboards.crazyhouse, PerfType.Crazyhouse),
          div(cls := "user_top")(
            h2(cls := "text", dataIcon := "g")(
              a(href := routes.Tournament.leaderboard)(trans.tournamentWinners())
            ),
            tourneyWinners take 10 map { w =>
              div(
                div(userIdLink(w.userId.some)),
                div(a(title := w.tourName, href := routes.Tournament.show(w.tourId))(
                  scheduledTournamentNameShortHtml(w.tourName)
                ))
              )
            }
          ),

          userTopPerf(leaderboards.blitz, PerfType.Blitz),
          userTopPerf(leaderboards.chess960, PerfType.Chess960),
          userTopActive(nbAllTime, trans.activePlayers(), icon = 'U'.some),

          userTopPerf(leaderboards.rapid, PerfType.Rapid),
          userTopPerf(leaderboards.threeCheck, PerfType.ThreeCheck),
          userTopPerf(leaderboards.antichess, PerfType.Antichess),

          userTopPerf(leaderboards.classical, PerfType.Classical),
          userTopPerf(leaderboards.kingOfTheHill, PerfType.KingOfTheHill),
          userTopPerf(leaderboards.horde, PerfType.Horde),

          userTopPerf(leaderboards.ultraBullet, PerfType.UltraBullet),
          userTopPerf(leaderboards.atomic, PerfType.Atomic),
          userTopPerf(leaderboards.racingKings, PerfType.RacingKings)
        )
      )
    }

  private def userTopPerf(users: List[User.LightPerf], perfType: PerfType) =
    div(cls := "user_top")(
      h2(cls := "text", dataIcon := perfType.iconChar)(
        a(href := routes.User.topNb(200, perfType.key))(perfType.name)
      ),
      users map { l =>
        div(
          div(lightUserLink(l.user)),
          div(l.rating)
        )
      }
    )

  private def userTopActive(users: List[User.LightCount], hTitle: Any, icon: Option[Char] = None)(implicit ctx: Context) =
    div(cls := "user_top")(
      h2(cls := "text", dataIcon := icon.map(_.toString))(hTitle.toString),
      users map { u =>
        div(
          div(lightUserLink(u.user)),
          div(title := trans.gamesPlayed.txt())(s"#${u.count.localize}")
        )
      }
    )

  private def side(online: List[User])(implicit ctx: Context) =
    div(cls := "side")(
      form(cls := "search public")(
        input(placeholder := trans.search.txt(), cls := "search_user user-autocomplete")
      ),
      isGranted(_.UserSearch) option form(cls := "search", action := routes.Mod.search())(
        input(name := "q", placeholder := "Search by IP, email, or username")
      ),
      div(cls := "user_lists")(
        div(cls := "user_top")(
          h2(trans.onlinePlayers()),
          online map { u =>
            div(
              div(userLink(u)),
              showBestPerf(u)
            )
          }
        )
      )
    )
}
