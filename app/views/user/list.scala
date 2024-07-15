package views.user

import lila.app.UiEnv.{ *, given }
import lila.core.perf.UserWithPerfs
import lila.rating.UserPerfsExt.bestAny3Perfs
import lila.user.LightCount

object list:

  private lazy val ui = lila.user.ui.UserList(helpers, bits)
  export ui.top

  def apply(
      tourneyWinners: List[lila.tournament.Winner],
      online: List[UserWithPerfs],
      leaderboards: lila.rating.UserPerfs.Leaderboards,
      nbAllTime: List[LightCount]
  )(using Context) =
    val winners = ol(
      tourneyWinners
        .take(10)
        .map: w =>
          li(
            userIdLink(w.userId.some),
            a(title := w.tourName, href := routes.Tournament.show(w.tourId)):
              views.tournament.ui.scheduledTournamentNameShortHtml(w.tourName)
          )
    )
    ui.page(online, leaderboards, nbAllTime, winners)

  def bots(users: List[UserWithPerfs])(using Context) = ui.bots(users, _.bestAny3Perfs)
