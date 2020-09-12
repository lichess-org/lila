package views.html
package userTournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

object chart {

  def apply(u: User, data: lila.tournament.LeaderboardApi.ChartData)(implicit ctx: Context) =
    bits.layout(
      u,
      title = s"${u.username} tournaments",
      path = "chart"
    ) {
      div(cls := "tournament-stats")(
        h1(cls := "box__pad")(userLink(u, withOnline = true), " tournament stats"),
        p(cls := "box__pad")(
          "The rank avg is a percentage of your ranking. Lower is better.",
          br,
          "For instance, being ranked 3 in a tournament of 100 players = 3%. ",
          "Being ranked 10 in a tournament of 1000 players = 1%."
        ),
        p(cls := "box__pad")(
          "All averages on this page are ",
          a(href := "http://dictionary.reference.com/help/faq/language/d72.html")("medians"),
          "."
        ),
        table(cls := "slist slist-pad perf-results")(
          thead(
            tr(
              th,
              th("Tournaments"),
              th("Points avg"),
              th("Points sum"),
              th("Rank avg")
            )
          ),
          tbody(
            data.perfResults.map {
              case (pt, res) =>
                tr(
                  th(iconTag(pt.iconChar, pt.trans)),
                  td(res.nb.localize),
                  td(res.points.median.map(_.toInt)),
                  td(res.points.sum.localize),
                  td(res.rankPercentMedian, "%")
                )
            },
            tr(
              th("Total"),
              td(data.allPerfResults.nb.localize),
              td(data.allPerfResults.points.median.map(_.toInt)),
              td(data.allPerfResults.points.sum.localize),
              td(data.allPerfResults.rankPercentMedian, "%")
            )
          )
        )
      )
    }
}
