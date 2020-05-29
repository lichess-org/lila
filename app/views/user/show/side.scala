package views.html.user.show

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.rating.PerfType
import lidraughts.user.User

import controllers.routes

object side {

  def apply(
    u: User,
    rankMap: lidraughts.rating.UserRankMap,
    active: Option[lidraughts.rating.PerfType]
  )(implicit ctx: Context) = {

    def showNonEmptyPerf(perf: lidraughts.rating.Perf, perfType: PerfType) =
      perf.nonEmpty option showPerf(perf, perfType)

    def showPerf(perf: lidraughts.rating.Perf, perfType: PerfType, name: Option[String] = none) = {
      val isGame = lidraughts.rating.PerfType.isGame(perfType)
      a(
        dataIcon := perfType.iconChar,
        title := perfType.title,
        cls := List(
          "empty" -> perf.isEmpty,
          "game" -> isGame,
          "active" -> active.has(perfType)
        ),
        href := isGame option routes.User.perfStat(u.username, perfType.key).url,
        span(
          h3(name.getOrElse(perfType.name).toUpperCase),
          st.rating(
            strong(
              perf.glicko.intRating,
              perf.provisional option "?"
            ),
            " ",
            ratingProgress(perf.progress),
            " ",
            span(
              if (perfType.key == "puzzle" || perfType.key == "puzzlefrisian" || perfType.key == "puzzlerussian") trans.nbPuzzles(perf.nb, perf.nb.localize)
              else trans.nbGames(perf.nb, perf.nb.localize)
            )
          ),
          rankMap get perfType map { rank =>
            span(cls := "rank", title := trans.rankIsUpdatedEveryNbMinutes.pluralSameTxt(15))(trans.rankX(rank.localize))
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
        showNonEmptyPerf(u.perfs.frisian, PerfType.Frisian),
        showNonEmptyPerf(u.perfs.frysk, PerfType.Frysk),
        showNonEmptyPerf(u.perfs.antidraughts, PerfType.Antidraughts),
        showNonEmptyPerf(u.perfs.breakthrough, PerfType.Breakthrough),
        br,
        showPerf(u.perfs.russian, PerfType.Russian),
        br,
        u.noBot option showPerf(u.perfs.puzzle(draughts.variant.Standard), PerfType.Puzzle),
        u.noBot option showNonEmptyPerf(u.perfs.puzzle(draughts.variant.Frisian), PerfType.PuzzleFrisian),
        u.noBot option showNonEmptyPerf(u.perfs.puzzle(draughts.variant.Russian), PerfType.PuzzleRussian)
      )
    )
  }
}
