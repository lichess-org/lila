package views.html.user

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue
import lidraughts.rating.PerfType
import lidraughts.user.User

import controllers.routes

object perfStat {

  def apply(
    u: User,
    rankMap: lidraughts.rating.UserRankMap,
    perfType: lidraughts.rating.PerfType,
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
          embedJsUnsafe(s"lidraughts.ratingHistoryChart($rc,'${perfType.name}');")
        )
      },
      jsAt(s"compiled/lidraughts.perfStat${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""$$(function() {
LidraughtsPerfStat(document.querySelector('.perf-stat__content'), {
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
          div(cls := "box__pad perf-stat__content")
        )
      )
    }
}
