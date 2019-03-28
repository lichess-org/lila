package views.html.user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.PerfType
import lila.user.User

import controllers.routes

object perfStat {

  def apply(
    u: User,
    rankMap: lila.rating.UserRankMap,
    perfType: lila.rating.PerfType,
    data: play.api.libs.json.JsObject,
    ratingChart: Option[String]
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${u.username} ${perfType.name} stats",
    side = Some(show.side(u, rankMap.some, perfType.some)),
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
      embedJs("""$(function() {
LichessPerfStat(document.getElementById('perfStatContent'), {
data: @toJsonHtml(data)
});
});""")
    ),
    moreCss = cssTag("user-perf-stat.css")
  ) {
      main(cls := s"page-menu ${perfType.key}", id := "perfStat")(
        st.aside(cls := "page-menu__menu")(show.side(u, rankMap.some, perfType.some)),
        div(cls := "page-menu__content box")(
          div(cls := "box__top")(
            u.perfs(perfType).nb > 0 option a(
              cls := "button text view_games",
              dataIcon := perfType.iconChar,
              href := s"${routes.User.games(u.username, "search")}?perf=${perfType.id}"
            )(
                "View the games"
              ),
            bits.perfTrophies(u, rankMap.filterKeys(perfType.key==).some),
            h1(
              a(href := routes.User.show(u.username), dataIcon := "I", cls := "text")(
                u.username, " ", span(perfType.name, " stats")
              )
            )
          ),
          ratingChart.isDefined option div(cls := "rating_history")(spinner),
          div(cls := "box__pad", id := "perfStatContent")
        )
      )
    }
}
