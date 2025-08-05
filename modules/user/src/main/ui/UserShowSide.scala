package lila.user
package ui

import lila.core.perf.PuzPerf
import lila.rating.UserWithPerfs.hasVariantRating
import lila.ui.*

import ScalatagsTemplate.{ *, given }
import scalalib.model.Days

final class UserShowSide(helpers: Helpers):
  import helpers.{ *, given }

  def apply(
      u: lila.core.perf.UserWithPerfs,
      rankMap: lila.rating.UserRankMap,
      active: Option[PerfKey]
  )(using ctx: Context) =

    def showNonEmptyPerf(perf: Perf, pk: PerfKey) =
      perf.nonEmpty.option(showPerf(perf, pk))

    def showPerf(perf: Perf, pk: PerfKey) =
      val isPuzzle = pk == PerfKey.puzzle
      a(
        dataIcon := pk.perfIcon,
        title := pk.perfDesc.txt(),
        cls := List(
          "empty" -> perf.isEmpty,
          "active" -> active.contains(pk)
        ),
        href := ctx.pref.showRatings.so:
          if isPuzzle
          then
            val other = ctx.isnt(u).option(u.username)
            routes.Puzzle.dashboard(Days(30), "home", other).url
          else routes.User.perfStat(u.username, pk).url
        ,
        span(
          h3(pk.perfTrans),
          if isPuzzle && lila.rating.ratingApi.dubiousPuzzle(u.perfs) && ctx.isnt(u) && ctx.pref.showRatings
          then st.rating(strong("?"))
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
                if pk == PerfKey.puzzle then trans.site.nbPuzzles.plural(perf.nb, perf.nb.localize)
                else trans.site.nbGames.plural(perf.nb, perf.nb.localize)
              )
            )
          ,
          rankMap.get(pk).ifTrue(ctx.pref.showRatings).map { rank =>
            span(cls := "rank", title := trans.site.rankIsUpdatedEveryNbMinutes.pluralSameTxt(15))(
              trans.site.rankX(rank.localize)
            )
          }
        ),
        ctx.pref.showRatings.option(iconTag(Icon.PlayTriangle))
      )

    div(cls := "side sub-ratings")(
      (!u.lame || ctx.is(u) || Granter.opt(_.UserModView)).option(
        frag(
          showNonEmptyPerf(u.perfs.ultraBullet, PerfKey.ultraBullet),
          showPerf(u.perfs.bullet, PerfKey.bullet),
          showPerf(u.perfs.blitz, PerfKey.blitz),
          showPerf(u.perfs.rapid, PerfKey.rapid),
          showPerf(u.perfs.classical, PerfKey.classical),
          showPerf(u.perfs.correspondence, PerfKey.correspondence),
          u.hasVariantRating.option(hr),
          showNonEmptyPerf(u.perfs.crazyhouse, PerfKey.crazyhouse),
          showNonEmptyPerf(u.perfs.chess960, PerfKey.chess960),
          showNonEmptyPerf(u.perfs.kingOfTheHill, PerfKey.kingOfTheHill),
          showNonEmptyPerf(u.perfs.threeCheck, PerfKey.threeCheck),
          showNonEmptyPerf(u.perfs.antichess, PerfKey.antichess),
          showNonEmptyPerf(u.perfs.atomic, PerfKey.atomic),
          showNonEmptyPerf(u.perfs.horde, PerfKey.horde),
          showNonEmptyPerf(u.perfs.racingKings, PerfKey.racingKings),
          u.noBot.option(
            frag(
              hr,
              showPerf(u.perfs.puzzle, PerfKey.puzzle),
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
