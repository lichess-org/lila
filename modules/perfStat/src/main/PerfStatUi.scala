package lila.perfStat

import play.api.libs.json.Json

import lila.common.Json.given
import lila.core.data.SafeJsonStr
import lila.core.perf.UserWithPerfs
import lila.core.perm.Granter
import lila.rating.PerfType
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class PerfStatUi(helpers: Helpers)(communityMenu: Context ?=> Frag):
  import helpers.{ *, given }
  import trans.perfStat as tps

  def page(data: PerfStatData, ratingChart: Option[SafeJsonStr], side: Frag, perfTrophies: Frag)(using
      Context
  ) =
    import data.{ user, stat }
    import stat.perfType
    Page(s"${user.username} - ${trans.perfStat.perfStats.txt(perfType.trans)}")
      .flag(_.noRobots)
      .js(Esm("user"))
      .js(ratingChart.map: rc =>
        esmInit(
          "chart.ratingHistory",
          SafeJsonStr(s"{data:$rc,singlePerfName:'${perfType.trans(using transDefault)}'}")
        ))
      .css("user.perf.stat"):
        main(cls := s"page-menu")(
          st.aside(cls := "page-menu__menu")(side),
          div(cls := s"page-menu__content box perf-stat ${perfType.key}")(
            boxTop(
              div(cls := "box__top__title")(
                perfTrophies,
                h1(
                  a(href := routes.User.show(user.username))(user.username),
                  span(tps.perfStats(perfType.trans))
                )
              ),
              div(cls := "box__top__actions")(
                a(
                  cls      := "button button-empty text",
                  dataIcon := perfType.icon,
                  href     := s"${routes.User.games(user.username, "search")}?perf=${perfType.id}"
                )(tps.viewTheGames())
              )
            ),
            ratingChart.isDefined.option(ratingHistoryContainer),
            content(data)
          )
        )

  private def percentileText(u: User, pk: PerfKey, percentile: Double)(using ctx: Context): Frag =
    if ctx.is(u) then
      trans.site.youAreBetterThanPercentOfPerfTypePlayers(
        a(href := routes.User.ratingDistribution(pk))(strong(percentile, "%")),
        a(href := routes.User.topNb(200, pk))(pk.perfName.txt())
      )
    else
      trans.site.userIsBetterThanPercentOfPerfTypePlayers(
        a(href := routes.User.show(u.username))(u.username),
        a(href := routes.User.ratingDistribution(pk, u.username.some))(
          strong(percentile, "%")
        ),
        a(href := routes.User.topNb(200, pk))(pk.perfName.txt())
      )

  def ratingHistoryContainer = div(cls := "rating-history-container")(
    div(cls := "rating-history-container")(
      div(cls := "time-selector-buttons"),
      spinner,
      div(cls := "chart-container")(canvas(cls := "rating-history")),
      div(id := "time-range-slider")
    )
  )

  def content(data: PerfStatData)(using Context): Frag =
    import data.*
    div(cls := "box__pad perf-stat__content")(
      glicko(user.user, stat.perfType, user.perfs(stat.perfType), percentile),
      counter(stat.count),
      highlow(stat, percentileLow, percentileHigh, user.user),
      resultStreak(stat.resultStreak, user.user),
      result(stat, user.user),
      playStreakNb(stat.playStreak, user.user),
      playStreakTime(stat.playStreak, user.user)
    )

  private def decimal(v: Double) = scalalib.Maths.roundDownAt(v, 2)

  private def glicko(u: User, pt: PerfType, perf: Perf, percentile: Option[Double])(using Context): Frag =
    st.section(cls := "glicko")(
      h2(
        trans.site.perfRatingX(
          strong(
            if perf.glicko.clueless then "?"
            else decimal(perf.glicko.rating).toString
          )
        ),
        perf.glicko.provisional.yes.option(
          frag(
            " ",
            span(
              title := tps.notEnoughRatedGames.txt(),
              cls   := "details"
            )("(", tps.provisional(), ")")
          )
        ),
        ". ",
        percentile.filter(_ != 0.0 && perf.glicko.provisional.no).map { percentile =>
          span(cls := "details")(
            percentileText(u, pt, percentile)
          )
        }
      ),
      p(
        tps.progressOverLastXGames(12),
        " ",
        span(cls := "progress")(
          if perf.progress.positive then tag("green")(dataIcon := Icon.ArrowUpRight)(perf.progress)
          else if perf.progress.negative then tag("red")(dataIcon := Icon.ArrowDownRight)(-perf.progress)
          else "-"
        ),
        ". ",
        tps.ratingDeviation(
          strong(
            title := tps.ratingDeviationTooltip.txt(
              chess.rating.glicko.provisionalDeviation,
              lila.rating.Glicko.standardRankableDeviation,
              lila.rating.Glicko.variantRankableDeviation
            )
          )(decimal(perf.glicko.deviation).toString)
        )
      )
    )

  private def pct(num: Int, denom: Int): String =
    (denom != 0).so(s"${Math.round(num * 100.0 / denom)}%")

  private def counter(count: lila.perfStat.Count)(using Translate): Frag =
    st.section(cls := "counter split")(
      div(
        table(
          tbody(
            tr(
              th(tps.totalGames()),
              td(count.all.localize),
              td
            ),
            tr(cls := "full")(
              th(tps.ratedGames()),
              td(count.rated.localize),
              td(pct(count.rated, count.all))
            ),
            tr(cls := "full")(
              th(tps.tournamentGames()),
              td(count.tour.localize),
              td(pct(count.tour, count.all))
            ),
            tr(cls := "full")(
              th(tps.berserkedGames()),
              td(count.berserk.localize),
              td(pct(count.berserk, count.tour))
            ),
            (count.seconds > 0).option(
              tr(cls := "full")(
                th(tps.timeSpentPlaying()),
                td(colspan := "2")(lila.core.i18n.translateDuration(count.duration))
              )
            )
          )
        )
      ),
      div(
        table(
          tbody(
            tr(
              th(tps.averageOpponent()),
              td(decimal(count.opAvg.avg).toString),
              td
            ),
            tr(cls := "full")(
              th(tps.victories()),
              td(tag("green")(count.win.localize)),
              td(tag("green")(pct(count.win, count.all)))
            ),
            tr(cls := "full")(
              th(trans.site.draws()),
              td(count.draw.localize),
              td(pct(count.draw, count.all))
            ),
            tr(cls := "full")(
              th(tps.defeats()),
              td(tag("red")(count.loss.localize)),
              td(tag("red")(pct(count.loss, count.all)))
            ),
            tr(cls := "full")(
              th(tps.disconnections()),
              td((count.disconnects > count.all * 100 / 15).option(tag("red")))(
                count.disconnects.localize
              ),
              td(pct(count.disconnects, count.all))
            )
          )
        )
      )
    )

  private def highlowSide(
      title: Frag => Frag,
      opt: Option[lila.perfStat.RatingAt],
      pctStr: Option[String],
      color: String,
      u: User
  )(using Translate): Frag = opt match
    case Some(r) =>
      div(
        h2(title(strong(tag(color)(r.int, pctStr.map(st.title := _))))),
        a(
          cls  := "glpt",
          href := s"${routes.Round.watcher(r.gameId, Color.white)}?pov=${u.username}"
        ):
          (absClientInstant(r.at))
      )
    case None => div(h2(title(emptyFrag)), " ", span(tps.notEnoughGames()))

  private def highlow(stat: PerfStat, pctLow: Option[Double], pctHigh: Option[Double], u: User)(using
      Translate
  ): Frag =
    import stat.perfType
    def titleOf(v: Double) = trans.site.betterThanPercentPlayers.txt(s"$v%", perfType.trans)
    st.section(cls := "highlow split")(
      highlowSide(tps.highestRating(_), stat.highest, pctHigh.filter(_ != 0.0).map(titleOf), "green", u),
      highlowSide(tps.lowestRating(_), stat.lowest, pctLow.filter(_ != 0.0).map(titleOf), "red", u)
    )

  private def fromTo(s: lila.perfStat.Streak, u: User)(using Translate): Frag =
    s.from match
      case Some(from) =>
        tps.fromXToY(
          a(
            cls  := "glpt",
            href := s"${routes.Round.watcher(from.gameId, Color.white)}?pov=${u.username}"
          ):
            (absClientInstant(from.at))
          ,
          s.to match
            case Some(to) =>
              a(
                cls  := "glpt",
                href := s"${routes.Round.watcher(to.gameId, Color.white)}?pov=${u.username}"
              ):
                (absClientInstant(to.at))
            case None => tps.now()
        )
      case None => nbsp

  private def resultStreakSideStreak(s: lila.perfStat.Streak, title: Frag => Frag, color: String, u: User)(
      using Translate
  ): Frag =
    div(cls := "streak")(
      h3(
        title(
          if s.v > 0 then tag(color)(trans.site.nbGames.plural(s.v, strong(s.v.localize)))
          else "-"
        )
      ),
      fromTo(s, u)
    )

  private def resultStreakSide(s: lila.perfStat.Streaks, title: Frag, color: String, u: User)(using
      Translate
  ): Frag =
    div(
      h2(title),
      resultStreakSideStreak(s.max, tps.longestStreak(_), color, u),
      resultStreakSideStreak(s.cur, tps.currentStreak(_), color, u)
    )

  private def resultStreak(streak: lila.perfStat.ResultStreak, u: User)(using Translate): Frag =
    st.section(cls := "resultStreak split")(
      resultStreakSide(streak.win, tps.winningStreak(), "green", u),
      resultStreakSide(streak.loss, tps.losingStreak(), "red", u)
    )

  private def resultTable(results: lila.perfStat.Results, title: Frag, user: User)(using Translate) =
    div:
      table(
        thead:
          tr(th(colspan := 2)(h2(title)))
        ,
        tbody:
          results.results.map: r =>
            tr(
              td(userIdLink(r.opId.some, withOnline = false), " (", r.opRating, ")"),
              td:
                a(
                  cls  := "glpt",
                  href := s"${routes.Round.watcher(r.gameId, Color.white)}?pov=${user.username}"
                ):
                  absClientInstant(r.at)
            )
      )

  private def result(stat: PerfStat, user: User)(using Context): Frag =
    st.section(cls := "result split")(
      resultTable(stat.bestWins, tps.bestRated(), user),
      (Granter.opt(_.BoostHunter) || Granter.opt(_.CheatHunter)).option(
        resultTable(
          stat.worstLosses,
          "Worst rated defeats",
          user
        )
      )
    )

  private def playStreakNbStreak(s: lila.perfStat.Streak, title: Frag => Frag, u: User)(using
      Translate
  ): Frag =
    div(
      div(cls := "streak")(
        h3(
          title(
            if s.v > 0 then trans.site.nbGames.plural(s.v, strong(s.v.localize))
            else "-"
          )
        ),
        fromTo(s, u)
      )
    )

  private def playStreakNbStreaks(streaks: lila.perfStat.Streaks, u: User)(using Translate): Frag =
    div(cls := "split")(
      playStreakNbStreak(streaks.max, tps.longestStreak(_), u),
      playStreakNbStreak(streaks.cur, tps.currentStreak(_), u)
    )

  private def playStreakNb(playStreak: lila.perfStat.PlayStreak, u: User)(using Translate): Frag =
    st.section(cls := "playStreak")(
      h2(span(title := tps.lessThanOneHour.txt())(tps.gamesInARow())),
      playStreakNbStreaks(playStreak.nb, u)
    )

  private def playStreakTimeStreak(s: lila.perfStat.Streak, title: Frag => Frag, u: User)(using
      Translate
  ): Frag =
    div(
      div(cls := "streak")(
        h3(title(lila.core.i18n.translateDuration(s.duration))),
        fromTo(s, u)
      )
    )

  private def playStreakTimeStreaks(streaks: lila.perfStat.Streaks, u: User)(using Translate): Frag =
    div(cls := "split")(
      playStreakTimeStreak(streaks.max, tps.longestStreak(_), u),
      playStreakTimeStreak(streaks.cur, tps.currentStreak(_), u)
    )

  private def playStreakTime(playStreak: lila.perfStat.PlayStreak, u: User)(using Translate): Frag =
    st.section(cls := "playStreak")(
      h2(span(title := tps.lessThanOneHour.txt())(tps.maxTimePlaying())),
      playStreakTimeStreaks(playStreak.time, u)
    )

  def ratingDistribution(perfType: PerfType, data: List[Int], otherUser: Option[UserWithPerfs])(using
      ctx: Context,
      me: Option[UserWithPerfs]
  ) =
    val myVisiblePerfs = me.map(_.perfs).ifTrue(ctx.pref.showRatings)
    Page(trans.site.weeklyPerfTypeRatingDistribution.txt(perfType.trans))
      .css("user.rating.stats")
      .flag(_.fullScreen)
      .js(
        PageModule(
          "chart.ratingDistribution",
          Json.obj(
            "freq"        -> data,
            "myRating"    -> myVisiblePerfs.map(_(perfType).intRating),
            "otherRating" -> otherUser.ifTrue(ctx.pref.showRatings).map(_.perfs(perfType).intRating),
            "otherPlayer" -> otherUser.map(_.username)
          )
        )
      ):
        main(cls := "page-menu")(
          communityMenu,
          div(cls := "rating-stats page-menu__content box box-pad")(
            boxTop(
              h1(
                trans.site.weeklyPerfTypeRatingDistribution(
                  lila.ui.bits.mselect(
                    "variant-stats",
                    span(perfType.trans),
                    lila.rating.PerfType.leaderboardable
                      .map(PerfType(_))
                      .map: pt =>
                        a(
                          dataIcon := pt.icon,
                          cls      := (perfType == pt).option("current"),
                          href     := routes.User.ratingDistribution(pt.key, otherUser.map(_.username))
                        )(pt.trans)
                  )
                )
              )
            ),
            div(cls := "desc", dataIcon := perfType.icon)(
              myVisiblePerfs
                .flatMap(_(perfType).glicko.establishedIntRating)
                .map: rating =>
                  val (under, sum) = lila.perfStat.percentileOf(data, rating)
                  div(
                    trans.site.nbPerfTypePlayersThisWeek(strong(sum.localize), perfType.trans),
                    br,
                    trans.site.yourPerfTypeRatingIsRating(perfType.trans, strong(rating)),
                    br,
                    trans.site.youAreBetterThanPercentOfPerfTypePlayers(
                      strong((under * 100.0 / sum).round, "%"),
                      perfType.trans
                    )
                  )
                .getOrElse:
                  div(
                    trans.site.nbPerfTypePlayersThisWeek
                      .plural(data.sum, strong(data.sum.localize), perfType.trans),
                    ctx.pref.showRatings.option(
                      frag(
                        br,
                        trans.site.youDoNotHaveAnEstablishedPerfTypeRating(perfType.trans)
                      )
                    )
                  )
            ),
            div(id := "rating_distribution")(
              canvas(
                id := "rating_distribution_chart",
                ariaTitle(trans.site.weeklyPerfTypeRatingDistribution.txt(perfType.trans))
              )(spinner)
            )
          )
        )
