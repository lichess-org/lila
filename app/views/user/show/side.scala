package views.html.user.show

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.PerfType
import lila.user.User

import controllers.routes

object side {

  def apply(
      u: User,
      rankMap: lila.rating.UserRankMap,
      active: Option[lila.rating.PerfType]
  )(implicit ctx: Context) = {

    def showNonEmptyPerf(perf: lila.rating.Perf, perfType: PerfType) =
      perf.nonEmpty option showPerf(perf, perfType)

    def showPerf(perf: lila.rating.Perf, perfType: PerfType) = {
      val isGame = lila.rating.PerfType.isGame(perfType)
      a(
        dataIcon := perfType.iconChar,
        title := perfType.desc,
        cls := List(
          "empty"  -> perf.isEmpty,
          "game"   -> isGame,
          "active" -> active.has(perfType)
        ),
        href := isGame option routes.User.perfStat(u.username, perfType.key).url,
        span(
          h3(perfType.trans),
          st.rating(
            strong(
              perf.glicko.intRating,
              perf.provisional option "?"
            ),
            " ",
            ratingProgress(perf.progress),
            " ",
            span(
              if (perfType.key == "puzzle") trans.nbPuzzles(perf.nb, perf.nb.localize)
              else trans.nbGames(perf.nb, perf.nb.localize)
            )
          ),
          rankMap get perfType map { rank =>
            span(cls := "rank", title := trans.rankIsUpdatedEveryNbMinutes.pluralSameTxt(15))(
              trans.rankX(rank.localize)
            )
          }
        ),
        isGame option iconTag("G")
      )
    }

    div(cls := "side sub-ratings")(
      (!u.lame || ctx.is(u) || isGranted(_.UserSpy)) option frag(
        showNonEmptyPerf(u.perfs.ultraBullet, PerfType.UltraBullet),
        showPerf(u.perfs.bullet, PerfType.Bullet),
        showPerf(u.perfs.blitz, PerfType.Blitz),
        showPerf(u.perfs.rapid, PerfType.Rapid),
        showPerf(u.perfs.classical, PerfType.Classical),
        showPerf(u.perfs.correspondence, PerfType.Correspondence),
        br,
        showNonEmptyPerf(u.perfs.crazyhouse, PerfType.Crazyhouse),
        showNonEmptyPerf(u.perfs.chess960, PerfType.Chess960),
        showNonEmptyPerf(u.perfs.kingOfTheHill, PerfType.KingOfTheHill),
        showNonEmptyPerf(u.perfs.threeCheck, PerfType.ThreeCheck),
        showNonEmptyPerf(u.perfs.antichess, PerfType.Antichess),
        showNonEmptyPerf(u.perfs.atomic, PerfType.Atomic),
        showNonEmptyPerf(u.perfs.horde, PerfType.Horde),
        showNonEmptyPerf(u.perfs.racingKings, PerfType.RacingKings),
        br,
        u.noBot option showPerf(u.perfs.puzzle, PerfType.Puzzle)
      )
    )
  }
}
