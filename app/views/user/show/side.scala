package views.html.user.show

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.rating.PerfType
import lila.user.User

object side:

  def apply(
      u: User.WithPerfs,
      rankMap: lila.rating.UserRankMap,
      active: Option[lila.rating.PerfType]
  )(using ctx: Context) =

    def showNonEmptyPerf(perf: lila.rating.Perf, perfType: PerfType) =
      perf.nonEmpty option showPerf(perf, perfType)

    def showPerf(perf: lila.rating.Perf, perfType: PerfType) =
      val isPuzzle = perfType == lila.rating.PerfType.Puzzle
      a(
        dataIcon := perfType.icon,
        title    := perfType.desc,
        cls := List(
          "empty"  -> perf.isEmpty,
          "active" -> active.has(perfType)
        ),
        href := ctx.pref.showRatings.so:
          if isPuzzle then routes.Puzzle.dashboard(30, "home", u.username.some).url
          else routes.User.perfStat(u.username, perfType.key).url
        ,
        span(
          h3(perfType.trans),
          if isPuzzle && u.perfs.dubiousPuzzle && !ctx.is(u) && ctx.pref.showRatings then
            st.rating(strong("?"))
          else
            st.rating(
              ctx.pref.showRatings option frag(
                if perf.glicko.clueless then strong("?")
                else
                  strong(
                    perf.glicko.intRating,
                    perf.provisional.yes option "?"
                  )
                ,
                " ",
                ratingProgress(perf.progress),
                " "
              ),
              span(
                if perfType.key.value == "puzzle" then trans.nbPuzzles.plural(perf.nb, perf.nb.localize)
                else trans.nbGames.plural(perf.nb, perf.nb.localize)
              )
            )
          ,
          rankMap get perfType ifTrue ctx.pref.showRatings map { rank =>
            span(cls := "rank", title := trans.rankIsUpdatedEveryNbMinutes.pluralSameTxt(15))(
              trans.rankX(rank.localize)
            )
          }
        ),
        ctx.pref.showRatings option iconTag(licon.PlayTriangle)
      )

    div(cls := "side sub-ratings")(
      (!u.lame || ctx.is(u) || isGranted(_.UserModView)) option frag(
        showNonEmptyPerf(u.perfs.ultraBullet, PerfType.UltraBullet),
        showPerf(u.perfs.bullet, PerfType.Bullet),
        showPerf(u.perfs.blitz, PerfType.Blitz),
        showPerf(u.perfs.rapid, PerfType.Rapid),
        showPerf(u.perfs.classical, PerfType.Classical),
        showPerf(u.perfs.correspondence, PerfType.Correspondence),
        u.hasVariantRating option hr,
        showNonEmptyPerf(u.perfs.crazyhouse, PerfType.Crazyhouse),
        showNonEmptyPerf(u.perfs.chess960, PerfType.Chess960),
        showNonEmptyPerf(u.perfs.kingOfTheHill, PerfType.KingOfTheHill),
        showNonEmptyPerf(u.perfs.threeCheck, PerfType.ThreeCheck),
        showNonEmptyPerf(u.perfs.antichess, PerfType.Antichess),
        showNonEmptyPerf(u.perfs.atomic, PerfType.Atomic),
        showNonEmptyPerf(u.perfs.horde, PerfType.Horde),
        showNonEmptyPerf(u.perfs.racingKings, PerfType.RacingKings),
        u.noBot option frag(
          hr,
          showPerf(u.perfs.puzzle, PerfType.Puzzle),
          showStorm(u.perfs.storm, u),
          showRacer(u.perfs.racer),
          showStreak(u.perfs.streak)
        )
      )
    )

  private def showStorm(storm: lila.rating.Perf.Storm, user: User)(using Lang) =
    a(
      dataIcon := licon.Storm,
      cls := List(
        "empty" -> !storm.nonEmpty
      ),
      href := routes.Storm.dashboardOf(user.username),
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
      iconTag(licon.PlayTriangle)
    )

  private def showRacer(racer: lila.rating.Perf.Racer)(using Lang) =
    a(
      dataIcon := licon.FlagChessboard,
      cls := List(
        "empty" -> !racer.nonEmpty
      ),
      href := routes.Racer.home,
      span(
        h3("Puzzle Racer"),
        st.rating(
          strong(racer.score),
          racer.nonEmpty option frag(
            " ",
            span(trans.storm.xRuns.plural(racer.runs, racer.runs.localize))
          )
        )
      ),
      iconTag(licon.PlayTriangle)
    )

  private def showStreak(streak: lila.rating.Perf.Streak)(using Lang) =
    a(
      dataIcon := licon.ArrowThruApple,
      cls := List(
        "empty" -> !streak.nonEmpty
      ),
      href := routes.Puzzle.streak,
      span(
        h3("Puzzle Streak"),
        st.rating(
          strong(streak.score),
          streak.nonEmpty option frag(
            " ",
            span(trans.storm.xRuns.plural(streak.runs, streak.runs.localize))
          )
        )
      ),
      iconTag(licon.PlayTriangle)
    )
