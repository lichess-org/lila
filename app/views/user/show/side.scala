package views.html.user.show

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.PerfType
import lila.user.User
import play.api.i18n.Lang

object side {

  def apply(
      u: User,
      rankMap: lila.rating.UserRankMap,
      active: Option[lila.rating.PerfType]
  )(implicit ctx: Context) = {

    def showNonEmptyPerf(perf: lila.rating.Perf, perfType: PerfType) =
      perf.nonEmpty option showPerf(perf, perfType)

    def showPerf(perf: lila.rating.Perf, perfType: PerfType) = {
      val isPuzzle = perfType == lila.rating.PerfType.Puzzle
      a(
        dataIcon := perfType.iconChar,
        title := perfType.desc,
        cls := List(
          "empty"  -> perf.isEmpty,
          "active" -> active.has(perfType)
        ),
        href := {
          if (isPuzzle) routes.Puzzle.dashboard(30, "home")
          else routes.User.perfStat(u.username, perfType.key)
        },
        span(
          h3(perfType.trans),
          st.rating(
            if (perf.glicko.clueless) strong("?")
            else
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
        iconTag("G")
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
        u.noBot option showPerf(u.perfs.puzzle, PerfType.Puzzle),
        u.noBot option showStorm(u.perfs.storm)
      )
    )
  }

  private def showStorm(storm: lila.rating.Perf.Storm)(implicit lang: Lang) =
    a(
      dataIcon := '~',
      cls := List(
        "empty" -> !storm.nonEmpty
      ),
      href := routes.Storm.dashboard(),
      span(
        h3("Puzzle Storm"),
        st.rating(
          strong(storm.score),
          storm.nonEmpty option frag(
            " ",
            span(trans.storm.xRuns.plural(storm.runs, storm.runs.localize))
          )
        )
      ),
      iconTag("G")
    )
}
