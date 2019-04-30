package views.html
package stat

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.PerfType

import controllers.routes

object ratingDistribution {

  def apply(perfType: PerfType, data: List[Int])(implicit ctx: Context) = views.html.base.layout(
    title = trans.weeklyPerfTypeRatingDistribution.txt(perfType.name),
    moreCss = cssTag("user.rating.stats"),
    wrapClass = "full-screen-force",
    moreJs = frag(
      jsTag("chart/ratingDistribution.js"),
      embedJsUnsafe(s"""lichess.ratingDistributionChart({
  freq: ${data.mkString("[", ",", "]")},
  myRating: ${ctx.me.fold("null")(_.perfs(perfType).intRating.toString)}
});""")
    )
  ) {
      main(cls := "page-menu")(
        user.bits.communityMenu("ratings"),
        div(cls := "rating-stats page-menu__content box box-pad")(
          h1(trans.weeklyPerfTypeRatingDistribution(views.html.base.bits.mselect(
            "variant-stats",
            span(perfType.name),
            PerfType.leaderboardable map { pt =>
              a(
                dataIcon := pt.iconChar,
                cls := (perfType == pt).option("current"),
                href := routes.Stat.ratingDistribution(pt.key)
              )(pt.name)
            }
          ))),
          div(cls := "desc", dataIcon := perfType.iconChar)(
            ctx.me.flatMap(_.perfs(perfType).glicko.establishedIntRating).map { rating =>
              lila.user.Stat.percentile(data, rating) match {
                case (under, sum) => div(
                  trans.nbPerfTypePlayersThisWeek(raw(s"""<strong>${sum.localize}</strong>"""), perfType.name), br,
                  trans.yourPerfTypeRatingIsRating(perfType.name, raw(s"""<strong>$rating</strong>""")), br,
                  trans.youAreBetterThanPercentOfPerfTypePlayers(raw(s"""<strong>${"%.1f" format under * 100.0 / sum}%</strong>"""), perfType.name)
                )
              }
            } getOrElse div(
              trans.nbPerfTypePlayersThisWeek.plural(data.sum, raw(s"""<strong>${data.sum.localize}</strong>"""), perfType.name), br,
              trans.youDoNotHaveAnEstablishedPerfTypeRating(perfType.name)
            )
          ),
          div(id := "rating_distribution_chart")(spinner)
        )
      )
    }

}
