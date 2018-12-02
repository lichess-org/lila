package views
package html.user

import scalatags.Text.all._

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.i18n.{ I18nKeys => trans }
import lidraughts.rating.PerfType
import lidraughts.user.User

import controllers.routes

object list {

  def apply(
    tourneyWinners: List[lidraughts.tournament.Winner],
    online: List[User],
    leaderboards: lidraughts.user.Perfs.Leaderboards,
    nbDay: List[User.LightCount],
    nbAllTime: List[User.LightCount]
  )(implicit ctx: Context) = layout(
    title = trans.players.txt(),
    side = Some(side(online)),
    openGraph = lidraughts.app.ui.OpenGraph(
      title = "Draughts players and leaderboards",
      url = s"$netBaseUrl${routes.User.list.url}",
      description = "Best draughts players in bullet, blitz, rapid, classical, Frisian, Antidraughts and more draughts variants"
    ).some,
    withInfScroll = false
  ) {
      div(cls := "content_box community")(
        communityTabs("leaderboard"),
        div(cls := "user_lists")(
          userTopPerf(leaderboards.bullet, PerfType.Bullet),
          userTopPerf(leaderboards.frisian, PerfType.Frisian),
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
          userTopPerf(leaderboards.antidraughts, PerfType.Antidraughts),
          userTopActive(nbAllTime, trans.activePlayers(), icon = 'U'.some),

          userTopPerf(leaderboards.rapid, PerfType.Rapid),
          userTopPerf(leaderboards.breakthrough, PerfType.Breakthrough),
          userTopPerf(leaderboards.frysk, PerfType.Frysk),

          userTopPerf(leaderboards.classical, PerfType.Classical),
          userTopPerf(leaderboards.ultraBullet, PerfType.UltraBullet),
          div(cls := "user_top")
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
