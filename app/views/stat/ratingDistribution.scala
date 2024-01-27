package views.html
package stat

import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.common.Json.given
import lila.rating.PerfType
import lila.user.{ User, UserPerfs }

import controllers.routes

object ratingDistribution:

  def apply(perfType: PerfType, data: List[Int], otherUser: Option[User.WithPerfs])(using
      ctx: PageContext,
      me: Option[User.WithPerfs]
  ) =
    val myVisiblePerfs = me.map(_.perfs) ifTrue ctx.pref.showRatings
    views.html.base.layout(
      title = trans.weeklyPerfTypeRatingDistribution.txt(perfType.trans),
      moreCss = cssTag("user.rating.stats"),
      wrapClass = "full-screen-force",
      moreJs = jsModuleInit(
        "chart.ratingDistribution",
        Json.obj(
          "freq"        -> data,
          "myRating"    -> myVisiblePerfs.map(_(perfType).intRating),
          "otherRating" -> otherUser.ifTrue(ctx.pref.showRatings).map(_.perfs(perfType).intRating),
          "otherPlayer" -> otherUser.map(_.username),
          "i18n"        -> i18nJsObject(i18nKeys)
        )
      )
    ) {
      main(cls := "page-menu")(
        user.bits.communityMenu("ratings"),
        div(cls := "rating-stats page-menu__content box box-pad")(
          boxTop(
            h1(
              trans.weeklyPerfTypeRatingDistribution(
                views.html.base.bits.mselect(
                  "variant-stats",
                  span(perfType.trans),
                  PerfType.leaderboardable map { pt =>
                    a(
                      dataIcon := pt.icon,
                      cls      := (perfType == pt).option("current"),
                      href     := routes.User.ratingDistribution(pt.key, otherUser.map(_.username))
                    )(pt.trans)
                  }
                )
              )
            )
          ),
          div(cls := "desc", dataIcon := perfType.icon)(
            myVisiblePerfs.flatMap(_(perfType).glicko.establishedIntRating).map { rating =>
              val (under, sum) = lila.user.Stat.percentile(data, rating)
              div(
                trans
                  .nbPerfTypePlayersThisWeek(strong(sum.localize), perfType.trans),
                br,
                trans.yourPerfTypeRatingIsRating(perfType.trans, strong(rating)),
                br,
                trans.youAreBetterThanPercentOfPerfTypePlayers(
                  strong((under * 100.0 / sum).round, "%"),
                  perfType.trans
                )
              )

            } getOrElse div(
              trans.nbPerfTypePlayersThisWeek
                .plural(data.sum, strong(data.sum.localize), perfType.trans),
              ctx.pref.showRatings option frag(
                br,
                trans.youDoNotHaveAnEstablishedPerfTypeRating(perfType.trans)
              )
            )
          ),
          div(id := "rating_distribution")(
            canvas(
              id := "rating_distribution_chart",
              ariaTitle(trans.weeklyPerfTypeRatingDistribution.txt(perfType.trans))
            )(spinner)
          )
        )
      )
    }

  private val i18nKeys = List(
    trans.players,
    trans.yourRating,
    trans.cumulative,
    trans.glicko2Rating
  )
