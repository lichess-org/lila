package views.html
package stat

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.PerfType

import controllers.routes

object ratingDistribution {

  def apply(perfType: PerfType, data: List[Int])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.monthlyPerfTypeRatingDistribution.txt(perfType.trans),
      moreCss = cssTag("user.rating.stats"),
      wrapClass = "full-screen-force",
      moreJs = frag(
        chartTag,
        moduleJsTag(
          "chart.rating-distribution",
          Json.obj(
            "freq"     -> data,
            "myRating" -> ctx.me.map(_.perfs(perfType).intRating)
          )
        )
      )
    ) {
      main(cls := "page-menu")(
        user.bits.communityMenu("ratings"),
        div(cls := "rating-stats page-menu__content box box-pad")(
          h1(
            trans.monthlyPerfTypeRatingDistribution(
              views.html.base.bits.mselect(
                "variant-stats",
                span(perfType.trans),
                PerfType.leaderboardable map { pt =>
                  a(
                    dataIcon := pt.iconChar,
                    cls      := (perfType == pt).option("current"),
                    href     := routes.Stat.ratingDistribution(pt.key)
                  )(pt.trans)
                }
              )
            )
          ),
          div(cls := "desc", dataIcon := perfType.iconChar)(
            ctx.me.flatMap(_.perfs(perfType).glicko.establishedIntRating).map { rating =>
              lila.user.Stat.percentile(data, rating) match {
                case (under, sum) =>
                  div(
                    trans
                      .nbPerfTypePlayersThisMonth(strong(sum.localize), perfType.trans),
                    br,
                    trans.yourPerfTypeRatingIsRating(perfType.trans, strong(rating)),
                    br,
                    trans.youAreBetterThanPercentOfPerfTypePlayers(
                      strong((under * 100.0 / sum).round, "%"),
                      perfType.trans
                    )
                  )
              }
            } getOrElse div(
              trans.nbPerfTypePlayersThisMonth
                .plural(data.sum, strong(data.sum.localize), perfType.trans),
              br,
              trans.youDoNotHaveAnEstablishedPerfTypeRating(perfType.trans)
            )
          ),
          div(id := "rating_distribution")(
            canvas(
              id := "rating_distribution_chart",
              ariaTitle(trans.monthlyPerfTypeRatingDistribution.txt(perfType.trans))
            )(spinner)
          )
        )
      )
    }

}
