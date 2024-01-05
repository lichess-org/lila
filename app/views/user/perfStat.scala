package views.html.user

import play.api.i18n.Lang
import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.rating.{ Perf, PerfType }
import lila.perfStat.{ PerfStat, PerfStatData }
import lila.user.User

import controllers.routes

object perfStat:

  import trans.perfStat.*

  def apply(
      data: PerfStatData,
      ratingChart: Option[String]
  )(using PageContext) =
    import data.*
    import stat.perfType
    views.html.base.layout(
      title = s"${user.username} - ${perfStats.txt(perfType.trans)}",
      robots = false,
      moreJs = frag(
        jsModule("user"),
        ratingChart.map: rc =>
          jsModuleInit(
            "chart.ratingHistory",
            s"{data:$rc,singlePerfName:'${perfType.trans(using lila.i18n.defaultLang)}'}"
          )
      ),
      moreCss = cssTag("perf-stat")
    ):
      main(cls := s"page-menu")(
        st.aside(cls := "page-menu__menu")(show.side(user, ranks, perfType.some)),
        div(cls := s"page-menu__content box perf-stat ${perfType.key}")(
          boxTop(
            div(cls := "box__top__title")(
              bits.perfTrophies(user, ranks.view.filterKeys(perfType == _).toMap),
              h1(
                a(href := routes.User.show(user.username))(user.username),
                span(perfStats(perfType.trans))
              )
            ),
            div(cls := "box__top__actions")(
              a(
                cls      := "button button-empty text",
                dataIcon := perfType.icon,
                href     := s"${routes.User.games(user.username, "search")}?perf=${perfType.id}"
              )(viewTheGames())
            )
          ),
          ratingChart.isDefined option ratingHistoryContainer,
          div(cls := "box__pad perf-stat__content")(
            glicko(user, perfType, user.perfs(perfType), percentile),
            counter(stat.count),
            highlow(stat, percentileLow, percentileHigh),
            resultStreak(stat.resultStreak),
            result(stat, user),
            playStreakNb(stat.playStreak),
            playStreakTime(stat.playStreak)
          )
        )
      )

  def ratingHistoryContainer = div(cls := "rating-history-container")(
    div(cls := "rating-history-container")(
      div(cls := "time-selector-buttons"),
      spinner,
      div(cls := "chart-container")(canvas(cls := "rating-history")),
      div(id := "time-range-slider")
    )
  )

  private def decimal(v: Double) = lila.common.Maths.roundDownAt(v, 2)

  private def glicko(u: User, perfType: PerfType, perf: Perf, percentile: Option[Double])(using
      ctx: Context
  ): Frag =
    st.section(cls := "glicko")(
      h2(
        trans.perfRatingX(
          strong(
            if perf.glicko.clueless then "?"
            else decimal(perf.glicko.rating).toString
          )
        ),
        perf.glicko.provisional.yes option frag(
          " ",
          span(
            title := notEnoughRatedGames.txt(),
            cls   := "details"
          )("(", provisional(), ")")
        ),
        ". ",
        percentile.filter(_ != 0.0 && perf.glicko.provisional.no).map { percentile =>
          span(cls := "details")(
            if ctx is u then
              trans.youAreBetterThanPercentOfPerfTypePlayers(
                a(href := routes.User.ratingDistribution(perfType.key))(strong(percentile, "%")),
                a(href := routes.User.topNb(200, perfType.key))(perfType.trans)
              )
            else
              trans.userIsBetterThanPercentOfPerfTypePlayers(
                a(href := routes.User.show(u.username))(u.username),
                a(href := routes.User.ratingDistribution(perfType.key, u.username.some))(
                  strong(percentile, "%")
                ),
                a(href := routes.User.topNb(200, perfType.key))(perfType.trans)
              )
          )
        }
      ),
      p(
        progressOverLastXGames(12),
        " ",
        span(cls := "progress")(
          if perf.progress > 0 then tag("green")(dataIcon := licon.ArrowUpRight)(perf.progress)
          else if perf.progress < 0 then tag("red")(dataIcon := licon.ArrowDownRight)(-perf.progress)
          else "-"
        ),
        ". ",
        ratingDeviation(
          strong(
            title := ratingDeviationTooltip.txt(
              lila.rating.Glicko.provisionalDeviation,
              lila.rating.Glicko.standardRankableDeviation,
              lila.rating.Glicko.variantRankableDeviation
            )
          )(decimal(perf.glicko.deviation).toString)
        )
      )
    )

  private def pct(num: Int, denom: Int): String =
    (denom != 0) so s"${Math.round(num * 100.0 / denom)}%"

  private def counter(count: lila.perfStat.Count)(using Lang): Frag =
    st.section(cls := "counter split")(
      div(
        table(
          tbody(
            tr(
              th(totalGames()),
              td(count.all),
              td
            ),
            tr(cls := "full")(
              th(ratedGames()),
              td(count.rated),
              td(pct(count.rated, count.all))
            ),
            tr(cls := "full")(
              th(tournamentGames()),
              td(count.tour),
              td(pct(count.tour, count.all))
            ),
            tr(cls := "full")(
              th(berserkedGames()),
              td(count.berserk),
              td(pct(count.berserk, count.tour))
            ),
            count.seconds > 0 option tr(cls := "full")(
              th(timeSpentPlaying()),
              td(colspan := "2")(showDuration(count.duration))
            )
          )
        )
      ),
      div(
        table(
          tbody(
            tr(
              th(averageOpponent()),
              td(decimal(count.opAvg.avg).toString),
              td
            ),
            tr(cls := "full")(
              th(victories()),
              td(tag("green")(count.win)),
              td(tag("green")(pct(count.win, count.all)))
            ),
            tr(cls := "full")(
              th(trans.draws()),
              td(count.draw),
              td(pct(count.draw, count.all))
            ),
            tr(cls := "full")(
              th(defeats()),
              td(tag("red")(count.loss)),
              td(tag("red")(pct(count.loss, count.all)))
            ),
            tr(cls := "full")(
              th(disconnections()),
              td(if count.disconnects > count.all * 100 / 15 then tag("red") else emptyFrag)(
                count.disconnects
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
      color: String
  )(using Lang): Frag = opt match
    case Some(r) =>
      div(
        h2(title(strong(tag(color)(r.int, pctStr.map(st.title := _))))),
        a(cls := "glpt", href := routes.Round.watcher(r.gameId, "white"))(absClientInstant(r.at))
      )
    case None => div(h2(title(emptyFrag)), " ", span(notEnoughGames()))

  private def highlow(stat: PerfStat, pctLow: Option[Double], pctHigh: Option[Double])(using Lang): Frag =
    import stat.perfType
    def titleOf(v: Double) = trans.betterThanPercentPlayers.txt(s"$v%", perfType.trans)
    st.section(cls := "highlow split")(
      highlowSide(highestRating(_), stat.highest, pctHigh.map(titleOf), "green"),
      highlowSide(lowestRating(_), stat.lowest, pctLow.map(titleOf), "red")
    )

  private def fromTo(s: lila.perfStat.Streak)(using Lang): Frag =
    s.from match
      case Some(from) =>
        fromXToY(
          a(cls := "glpt", href := routes.Round.watcher(from.gameId, "white"))(absClientInstant(from.at)),
          s.to match
            case Some(to) =>
              a(cls := "glpt", href := routes.Round.watcher(to.gameId, "white"))(absClientInstant(to.at))
            case None => now()
        )
      case None => nbsp

  private def resultStreakSideStreak(s: lila.perfStat.Streak, title: Frag => Frag, color: String)(using
      Lang
  ): Frag =
    div(cls := "streak")(
      h3(
        title(
          if s.v > 0 then tag(color)(trans.nbGames.plural(s.v, strong(s.v)))
          else "-"
        )
      ),
      fromTo(s)
    )

  private def resultStreakSide(s: lila.perfStat.Streaks, title: Frag, color: String)(using
      Lang
  ): Frag =
    div(
      h2(title),
      resultStreakSideStreak(s.max, longestStreak(_), color),
      resultStreakSideStreak(s.cur, currentStreak(_), color)
    )

  private def resultStreak(streak: lila.perfStat.ResultStreak)(using Lang): Frag =
    st.section(cls := "resultStreak split")(
      resultStreakSide(streak.win, winningStreak(), "green"),
      resultStreakSide(streak.loss, losingStreak(), "red")
    )

  private def resultTable(results: lila.perfStat.Results, title: Frag, user: User)(using
      Lang
  ): Frag =
    div(
      table(
        thead(
          tr(
            th(colspan := 2)(h2(title))
          )
        ),
        tbody(
          results.results map { r =>
            tr(
              td(userIdLink(r.opId.some, withOnline = false), " (", r.opRating, ")"),
              td(
                a(cls := "glpt", href := s"${routes.Round.watcher(r.gameId, "white")}?pov=${user.username}")(
                  absClientInstant(r.at)
                )
              )
            )
          }
        )
      )
    )

  private def result(stat: PerfStat, user: User)(using Lang): Frag =
    st.section(cls := "result split")(
      resultTable(stat.bestWins, bestRated(), user),
      resultTable(stat.worstLosses, worstRated(), user)
    )

  private def playStreakNbStreak(s: lila.perfStat.Streak, title: Frag => Frag)(using Lang): Frag =
    div(
      div(cls := "streak")(
        h3(
          title(
            if s.v > 0 then trans.nbGames.plural(s.v, strong(s.v))
            else "-"
          )
        ),
        fromTo(s)
      )
    )

  private def playStreakNbStreaks(streaks: lila.perfStat.Streaks)(using Lang): Frag =
    div(cls := "split")(
      playStreakNbStreak(streaks.max, longestStreak(_)),
      playStreakNbStreak(streaks.cur, currentStreak(_))
    )

  private def playStreakNb(playStreak: lila.perfStat.PlayStreak)(using Lang): Frag =
    st.section(cls := "playStreak")(
      h2(span(title := lessThanOneHour.txt())(gamesInARow())),
      playStreakNbStreaks(playStreak.nb)
    )

  private def playStreakTimeStreak(s: lila.perfStat.Streak, title: Frag => Frag)(using Lang): Frag =
    div(
      div(cls := "streak")(
        h3(title(showDuration(s.duration))),
        fromTo(s)
      )
    )

  private def playStreakTimeStreaks(streaks: lila.perfStat.Streaks)(using Lang): Frag =
    div(cls := "split")(
      playStreakTimeStreak(streaks.max, longestStreak(_)),
      playStreakTimeStreak(streaks.cur, currentStreak(_))
    )

  private def playStreakTime(playStreak: lila.perfStat.PlayStreak)(using Lang): Frag =
    st.section(cls := "playStreak")(
      h2(span(title := lessThanOneHour.txt())(maxTimePlaying())),
      playStreakTimeStreaks(playStreak.time)
    )
