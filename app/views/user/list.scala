package views.user

import lila.app.templating.Environment.{ *, given }
import lila.rating.PerfType
import lila.rating.UserPerfsExt.bestAny3Perfs
import lila.user.LightCount
import lila.core.perf.UserWithPerfs

object list:

  private lazy val ui = lila.user.ui.UserList(helpers, bits)
  import ui.*

  def apply(
      tourneyWinners: List[lila.tournament.Winner],
      online: List[UserWithPerfs],
      leaderboards: lila.rating.UserPerfs.Leaderboards,
      nbAllTime: List[LightCount]
  )(using ctx: PageContext) =
    views.base.layout(
      title = trans.site.players.txt(),
      moreCss = cssTag("user.list"),
      wrapClass = "full-screen-force",
      openGraph = OpenGraph(
        title = "Chess players and leaderboards",
        url = s"$netBaseUrl${routes.User.list.url}",
        description =
          "Best chess players in bullet, blitz, rapid, classical, Chess960 and more chess variants"
      ).some
    )(ui.page(online, leaderboards, nbAllTime, tournamentWinners(tourneyWinners)))

  private def tournamentWinners(winners: List[lila.tournament.Winner])(using Context) =
    ol(
      winners
        .take(10)
        .map: w =>
          li(
            userIdLink(w.userId.some),
            a(title := w.tourName, href := routes.Tournament.show(w.tourId)):
              views.tournament.ui.scheduledTournamentNameShortHtml(w.tourName)
          )
    )

  def top(perfType: PerfType, users: List[lila.core.user.LightPerf])(using ctx: PageContext) =
    val title = s"${perfType.trans} top 200"
    views.base.layout(
      title = title,
      moreCss = cssTag("slist"),
      openGraph = OpenGraph(
        title = s"Leaderboard of ${perfType.trans}",
        url = s"$netBaseUrl${routes.User.topNb(200, perfType.key).url}",
        description = s"The 200 best chess players in ${perfType.trans}, sorted by rating"
      ).some
    )(ui.top(users, title))

  def bots(users: List[UserWithPerfs])(using PageContext) =
    val title = s"${users.size} Online bots"
    views.base.layout(
      title = title,
      moreCss = frag(cssTag("slist"), cssTag("bot.list")),
      wrapClass = "full-screen-force"
    )(ui.bots(users, title, _.bestAny3Perfs))
