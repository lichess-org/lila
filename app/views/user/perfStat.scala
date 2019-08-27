package views.html.user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.rating.{ Perf, PerfType }
import lila.perfStat.PerfStat
import lila.user.User

import controllers.routes

object perfStat {

  def apply(
    u: User,
    rankMap: lila.rating.UserRankMap,
    perfType: lila.rating.PerfType,
    percentile: Option[Double],
    stat: PerfStat,
    data: play.api.libs.json.JsObject,
    ratingChart: Option[String]
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${u.username} ${perfType.name} stats",
    robots = false,
    moreJs = frag(
      jsAt("compiled/user.js"),
      ratingChart.map { rc =>
        frag(
          jsTag("chart/ratingHistory.js"),
          embedJsUnsafe(s"lichess.ratingHistoryChart($rc,'${perfType.name}');")
        )
      },
      jsAt(s"compiled/lichess.perfStat${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""$$(function() {
if (false) LichessPerfStat(document.querySelector('.perf-stat__content'), {
data: ${safeJsonValue(data)}
});
});""")
    ),
    moreCss = cssTag("perf-stat")
  ) {
      main(cls := s"page-menu")(
        st.aside(cls := "page-menu__menu")(show.side(u, rankMap, perfType.some)),
        div(cls := s"page-menu__content box perf-stat ${perfType.key}")(
          div(cls := "box__top")(
            h1(
              a(href := routes.User.show(u.username))(u.username),
              span(perfType.name, " stats")
            ),
            div(cls := "box__top__actions")(
              u.perfs(perfType).nb > 0 option a(
                cls := "button button-empty text",
                dataIcon := perfType.iconChar,
                href := s"${routes.User.games(u.username, "search")}?perf=${perfType.id}"
              )("View the games"),
              bits.perfTrophies(u, rankMap.filterKeys(perfType==))
            )
          ),
          ratingChart.isDefined option div(cls := "rating-history")(spinner),
          div(cls := "box__pad perf-stat__content")(
            glicko(perfType, u.perfs(perfType), percentile),
            counter(stat.count),
            highlow(stat),
            resultStreak(stat.resultStreak),
            result(stat),
            playStreakNb(stat.playStreak),
            playStreakTime(stat.playStreak)
          )
        )
      )
    }

  private def decimal(v: Double) = lila.common.Maths.roundAt(v, 2)

  private def glicko(perfType: PerfType, perf: Perf, percentile: Option[Double]): Frag = st.section(cls := "glicko")(
    h2(
      "Rating: ",
      strong(title := "Yes, ratings have decimal accuracy.")(decimal(perf.glicko.rating).toString),
      ". ",
      span(cls := "details")(
        perf.glicko.provisional option span(title := "Not enough rated games have been played to establish a reliable rating.")("(provisional)"),
        percentile.filter(_ != 0.0 && !perf.glicko.provisional) map { percentile =>
          frag(
            "Better than ",
            a(href := routes.Stat.ratingDistribution(perfType.key))(
              strong(percentile, "%"), " of ", perfType.name, " players"
            ),
            "."
          )
        }
      )
    ),
    p(
      "Progression over the last twelve games: ",
      span(cls := "progress")(
        if (perf.progress > 0) tag("green")(dataIcon := "N")(perf.progress)
        else if (perf.progress < 0) tag("red")(dataIcon := "M")(-perf.progress)
        else frag("none")
      ),
      ". ",
      "Rating deviation: ",
      strong(title := "Lower value means the rating is more stable. Above 110, the rating is considered provisional.")(decimal(perf.glicko.deviation).toString),
      "."
    )
  )

  private def pct(num: Int, denom: Int): String = {
    if (denom == 0) "0"
    else s"${Math.round(num * 100 / denom)}%"
  }

  private def counter(count: lila.perfStat.Count): Frag = st.section(cls := "counter split")(
    div(
      table(
        tbody(
          tr(
            th("Total games"),
            td(count.all),
            td
          ),
          tr(cls := "full")(
            th("Rated games"),
            td(count.rated),
            td(pct(count.rated, count.all))
          ),
          tr(cls := "full")(
            th("Tournament games"),
            td(count.tour),
            td(pct(count.tour, count.all))
          ),
          tr(cls := "full")(
            th("Berserked games"),
            td(count.berserk),
            td(pct(count.berserk, count.tour))
          ),
          count.seconds > 0 option tr(cls := "full")(
            th("Time spent playing"),
            td(colspan := "2") {
              val hours = count.seconds / (60 * 60)
              val minutes = (count.seconds % (60 * 60)) / 60
              s"${hours}h, ${minutes}m"
            }
          )
        )
      )
    ),
    div(
      table(
        tbody(
          tr(
            th("Average opponent"),
            td(decimal(count.opAvg.avg).toString),
            td
          ),
          tr(cls := "full")(
            th("Victories"),
            td(tag("green")(count.win)),
            td(tag("green")(pct(count.win, count.all)))
          ),
          tr(cls := "full")(
            th("Draws"),
            td(count.draw),
            td(pct(count.draw, count.all))
          ),
          tr(cls := "full")(
            th("Defeats"),
            td(tag("red")(count.loss)),
            td(tag("red")(pct(count.loss, count.all)))
          ),
          tr(cls := "full")(
            th("Disconnections"),
            td(if (count.disconnects > count.all * 100 / 15) tag("red") else frag())(count.disconnects),
            td(pct(count.disconnects, count.all))
          )
        )
      )
    )
  )

  private def highlowSide(title: String, opt: Option[lila.perfStat.RatingAt], color: String)(implicit ctx: Context): Frag =
    opt match {
      case Some(r) => div(
        h2(title, ": ", strong(tag(color)(r.int))),
        a(cls := "glpt", href := routes.Round.watcher(r.gameId, "white"))(absClientDateTime(r.at))
      )
      case None => div(h2(title), " ", span("Not enough games played"))
    }

  private def highlow(stat: PerfStat)(implicit ctx: Context): Frag = st.section(cls := "highlow split")(
    highlowSide("Highest rating", stat.highest, "green"),
    highlowSide("Lowest rating", stat.lowest, "red")
  )

  private def fromTo(s: lila.perfStat.Streak)(implicit ctx: Context): Frag =
    s.from match {
      case Some(from) => frag(
        "from ",
        a(cls := "glpt", href := routes.Round.watcher(from.gameId, "white"))(absClientDateTime(from.at)),
        " to ",
        s.to match {
          case Some(to) => a(cls := "glpt", href := routes.Round.watcher(to.gameId, "white"))(absClientDateTime(to.at))
          case None => frag("now")
        }
      )
      case None => nbsp
    }

  private def resultStreakSideStreak(s: lila.perfStat.Streak, title: String, color: String)(implicit ctx: Context): Frag = div(cls := "streak")(
    h3(
      title, ": ",
      if (s.v == 1) tag(color)(frag(strong(s.v), " game"))
      else if (s.v > 0) tag(color)(frag(strong(s.v), " games"))
      else frag("none")
    ),
    fromTo(s)
  )

  private def resultStreakSide(s: lila.perfStat.Streaks, title: String, color: String)(implicit ctx: Context): Frag = div(
    h2(title),
    resultStreakSideStreak(s.max, "Longest", color),
    resultStreakSideStreak(s.cur, "Current", color)
  )

  private def resultStreak(streak: lila.perfStat.ResultStreak)(implicit ctx: Context): Frag = st.section(cls := "resultStreak split")(
    resultStreakSide(streak.win, "Winning streak", "green"),
    resultStreakSide(streak.loss, "Losing streak", "red")
  )

  private def resultTable(results: lila.perfStat.Results, title: String)(implicit ctx: Context): Frag = div(
    table(
      thead(
        tr(
          th(colspan := 2)(h2(title))
        )
      ),
      tbody(
        results.results map { r =>
          tr(
            td(userIdLink(r.opId.value, none)),
            td(a(cls := "glpt", href := routes.Round.watcher(r.gameId, "white"))(absClientDateTime(r.at)))
          )
        }
      )
    )
  )

  private def result(stat: PerfStat)(implicit ctx: Context): Frag = st.section(cls := "result split")(
    resultTable(stat.bestWins, "Best rated victories"),
    resultTable(stat.worstLosses, "Worst rated defeats")
  )

  private def playStreakNbStreak(s: lila.perfStat.Streak, title: String)(implicit ctx: Context): Frag = div(
    div(cls := "streak")(
      h3(
        title, ": ",
        if (s.v == 1) frag(strong(s.v), " game")
        else if (s.v > 0) frag(strong(s.v), " games")
        else frag("none")
      ),
      fromTo(s)
    )
  )

  private def playStreakNbStreaks(streaks: lila.perfStat.Streaks)(implicit ctx: Context): Frag = div(cls := "split")(
    playStreakNbStreak(streaks.max, "Longest streak"),
    playStreakNbStreak(streaks.cur, "Current streak")
  )

  private def playStreakNb(playStreak: lila.perfStat.PlayStreak)(implicit ctx: Context): Frag = st.section(cls := "playStreak")(
    h2(span(title := "Less than one hour between games")("Games played in a row")),
    playStreakNbStreaks(playStreak.nb)
  )

  private def playStreakTimeStreak(s: lila.perfStat.Streak, title: String)(implicit ctx: Context): Frag = div(
    div(cls := "streak")(
      h3(
        title, ": ", {
        val hours = s.v / (60 * 60)
        val minutes = (s.v % (60 * 60)) / 60
        s"${hours} hours, ${minutes} minutes"
      }
      ),
      fromTo(s)
    )
  )

  private def playStreakTimeStreaks(streaks: lila.perfStat.Streaks)(implicit ctx: Context): Frag = div(cls := "split")(
    playStreakTimeStreak(streaks.max, "Longest streak"),
    playStreakTimeStreak(streaks.cur, "Current streak")
  )

  private def playStreakTime(playStreak: lila.perfStat.PlayStreak)(implicit ctx: Context): Frag = st.section(cls := "playStreak")(
    h2(span(title := "Less than one hour between games")("Max time spent playing")),
    playStreakTimeStreaks(playStreak.time)

  )
}
