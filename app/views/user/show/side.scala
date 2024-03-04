package views.html.user.show

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.PerfType
import lila.user.User

import play.api.i18n.Lang

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
      val isPuzzle = perfType == lila.rating.PerfType.Puzzle
      a(
        dataIcon := perfType.iconChar,
        title    := perfType.desc,
        cls := List(
          "perf-item" -> true,
          "empty"     -> perf.isEmpty,
          "active"    -> active.has(perfType)
        ),
        href := {
          if (isPuzzle) ctx.is(u) option routes.Puzzle.dashboard(30, "home").url
          else routes.User.perfStat(u.username, perfType.key).url.some
        },
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
              if (perfType.key == "puzzle") trans.nbPuzzles.plural(perf.nb, perf.nb.localize)
              else trans.nbGames.plural(perf.nb, perf.nb.localize)
            )
          ),
          rankMap get perfType map { rank =>
            span(cls := "rank", title := trans.rankIsUpdatedEveryNbMinutes.pluralSameTxt(15))(
              trans.rankX(rank.localize)
            )
          }
        ),
        !isPuzzle option iconTag("G")
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
        showNonEmptyPerf(u.perfs.minishogi, PerfType.Minishogi),
        showNonEmptyPerf(u.perfs.chushogi, PerfType.Chushogi),
        showNonEmptyPerf(u.perfs.annanshogi, PerfType.Annanshogi),
        showNonEmptyPerf(u.perfs.kyotoshogi, PerfType.Kyotoshogi),
        showNonEmptyPerf(u.perfs.checkshogi, PerfType.Checkshogi),
        br,
        u.noBot option showPerf(u.perfs.puzzle, PerfType.Puzzle),
        u.noBot option showStorm(u.perfs.storm, u),
        u.noBot option br,
        u.perfs.aiLevels.standard.ifTrue(u.noBot).map(l => aiLevel(l, shogi.variant.Standard)),
        u.perfs.aiLevels.minishogi.ifTrue(u.noBot).map(l => aiLevel(l, shogi.variant.Minishogi)),
        u.perfs.aiLevels.kyotoshogi.ifTrue(u.noBot).map(l => aiLevel(l, shogi.variant.Kyotoshogi))
      )
    )
  }

  private def showStorm(storm: lila.rating.Perf.Storm, user: User)(implicit lang: Lang) =
    a(
      dataIcon := '.',
      cls := List(
        "perf-item" -> true,
        "empty"     -> !storm.nonEmpty
      ),
      href := routes.Storm.dashboardOf(user.username),
      span(
        h3("Storm"),
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

  private def aiLevel(level: Int, variant: shogi.variant.Variant)(implicit lang: Lang) =
    div(
      dataIcon := 'n',
      cls      := s"perf-item ai-level ai-level-$level",
      span(
        h3(variantName(variant)),
        div(cls := "ai-level")(
          trans.defeatedAiNameAiLevel.txt("AI", level)
        )
      )
    )
}
