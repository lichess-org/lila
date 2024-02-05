package views.html
package userTournament

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.user.User

object chart:

  def apply(u: User, data: lila.tournament.LeaderboardApi.ChartData)(using PageContext) =
    bits.layout(
      u,
      title = s"${u.username} tournaments",
      path = "chart"
    ) {
      div(cls := "tournament-stats")(
        boxTop(h1(trans.xTournamentStats.txt(userLink(u, withOnline = true)))),
        p(cls := "box__pad")(trans.rankAvgHelp("3%", "1%")),
        p(cls := "box__pad")(
          trans.allaveragesAreX(
            a(href := "https://www.dictionary.com/e/average-vs-mean-vs-median-vs-mode")(trans.medians())
          )
        ),
        table(cls := "slist slist-pad perf-results")(
          thead(
            tr(
              th,
              th(trans.tournaments()),
              th(trans.pointsAvg()),
              th(trans.pointsSum()),
              th(trans.rankAvg())
            )
          ),
          tbody(
            data.perfResults.map { case (pt, res) =>
              tr(
                th(iconTag(pt.icon, pt.trans)),
                td(res.nb.localize),
                td(res.points.median.map(_.toInt)),
                td(res.points.sum.localize),
                td(res.rankPercentMedian, "%")
              )
            },
            tr(
              th(trans.total()),
              td(data.allPerfResults.nb.localize),
              td(data.allPerfResults.points.median.map(_.toInt)),
              td(data.allPerfResults.points.sum.localize),
              td(data.allPerfResults.rankPercentMedian, "%")
            )
          )
        )
      )
    }
