package views.html.user.show

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.rating.PerfType
import lila.rating.{ PerfType as PTs }
import lila.core.perf.{ UserWithPerfs, PuzPerf }
import lila.rating.GlickoExt.clueless
import lila.rating.UserPerfsExt.dubiousPuzzle
import lila.rating.UserWithPerfs.hasVariantRating

object side:

  def apply(
      u: UserWithPerfs,
      rankMap: lila.rating.UserRankMap,
      active: Option[PerfType]
  )(using ctx: Context) =

    def showNonEmptyPerf(perf: Perf, perfType: PerfType) =
      perf.nonEmpty.option(showPerf(perf, perfType))

    def showPerf(perf: Perf, perfType: PerfType) =
      val isPuzzle = perfType == PerfType.Puzzle
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
          if isPuzzle && u.perfs.dubiousPuzzle && ctx.isnt(u) && ctx.pref.showRatings then
            st.rating(strong("?"))
          else
            st.rating(
              ctx.pref.showRatings.option(
                frag(
                  if perf.glicko.clueless then strong("?")
                  else
                    strong(
                      perf.glicko.intRating,
                      perf.provisional.yes.option("?")
                    )
                  ,
                  " ",
                  ratingProgress(perf.progress),
                  " "
                )
              ),
              span(
                if perfType.key.value == "puzzle" then trans.site.nbPuzzles.plural(perf.nb, perf.nb.localize)
                else trans.site.nbGames.plural(perf.nb, perf.nb.localize)
              )
            )
          ,
          rankMap.get(perfType).ifTrue(ctx.pref.showRatings).map { rank =>
            span(cls := "rank", title := trans.site.rankIsUpdatedEveryNbMinutes.pluralSameTxt(15))(
              trans.site.rankX(rank.localize)
            )
          }
        ),
        ctx.pref.showRatings.option(iconTag(Icon.PlayTriangle))
      )

    div(cls := "side sub-ratings")(
      (!u.lame || ctx.is(u) || isGranted(_.UserModView)).option(
        frag(
          showNonEmptyPerf(u.perfs.ultraBullet, PTs.UltraBullet),
          showPerf(u.perfs.bullet, PTs.Bullet),
          showPerf(u.perfs.blitz, PTs.Blitz),
          showPerf(u.perfs.rapid, PTs.Rapid),
          showPerf(u.perfs.classical, PTs.Classical),
          showPerf(u.perfs.correspondence, PTs.Correspondence),
          u.hasVariantRating.option(hr),
          showNonEmptyPerf(u.perfs.crazyhouse, PTs.Crazyhouse),
          showNonEmptyPerf(u.perfs.chess960, PTs.Chess960),
          showNonEmptyPerf(u.perfs.kingOfTheHill, PTs.KingOfTheHill),
          showNonEmptyPerf(u.perfs.threeCheck, PTs.ThreeCheck),
          showNonEmptyPerf(u.perfs.antichess, PTs.Antichess),
          showNonEmptyPerf(u.perfs.atomic, PTs.Atomic),
          showNonEmptyPerf(u.perfs.horde, PTs.Horde),
          showNonEmptyPerf(u.perfs.racingKings, PTs.RacingKings),
          u.noBot.option(
            frag(
              hr,
              showPerf(u.perfs.puzzle, PTs.Puzzle),
              showStorm(u.perfs.storm, u),
              showRacer(u.perfs.racer),
              showStreak(u.perfs.streak)
            )
          )
        )
      )
    )

  private def showStorm(storm: PuzPerf, user: User)(using Translate) =
    a(
      dataIcon := Icon.Storm,
      cls := List(
        "empty" -> !storm.nonEmpty
      ),
      href := routes.Storm.dashboardOf(user.username),
      span(
        h3("Puzzle Storm"),
        st.rating(
          strong(storm.score),
          storm.nonEmpty.option(
            frag(
              " ",
              span(trans.storm.xRuns.plural(storm.runs, storm.runs.localize))
            )
          )
        )
      ),
      iconTag(Icon.PlayTriangle)
    )

  private def showRacer(racer: PuzPerf)(using Translate) =
    a(
      dataIcon := Icon.FlagChessboard,
      cls := List(
        "empty" -> !racer.nonEmpty
      ),
      href := routes.Racer.home,
      span(
        h3("Puzzle Racer"),
        st.rating(
          strong(racer.score),
          racer.nonEmpty.option(
            frag(
              " ",
              span(trans.storm.xRuns.plural(racer.runs, racer.runs.localize))
            )
          )
        )
      ),
      iconTag(Icon.PlayTriangle)
    )

  private def showStreak(streak: PuzPerf)(using Translate) =
    a(
      dataIcon := Icon.ArrowThruApple,
      cls := List(
        "empty" -> !streak.nonEmpty
      ),
      href := routes.Puzzle.streak,
      span(
        h3("Puzzle Streak"),
        st.rating(
          strong(streak.score),
          streak.nonEmpty.option(
            frag(
              " ",
              span(trans.storm.xRuns.plural(streak.runs, streak.runs.localize))
            )
          )
        )
      ),
      iconTag(Icon.PlayTriangle)
    )
