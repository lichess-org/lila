package views.html.user.show

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.mashup.UserInfo
import lila.app.mashup.UserInfo.Angle
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.game.Game
import lila.user.User

object page {

  def activity(
      u: User,
      activities: Vector[lila.activity.ActivityView],
      info: UserInfo,
      social: lila.app.mashup.UserInfo.Social
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username} : ${trans.activity.activity.txt()}",
      openGraph = lila.app.ui
        .OpenGraph(
          image = assetUrl("logo/lichess-tile-wide.png").some,
          twitterImage = assetUrl("logo/lichess-tile.png").some,
          title = u.titleUsernameWithBestRating,
          url = s"$netBaseUrl${routes.User.show(u.username).url}",
          description = describeUser(u)
        )
        .some,
      moreJs = moreJs(info),
      moreCss = frag(
        cssTag("user.show"),
        isGranted(_.UserModView) option cssTag("mod.user")
      ),
      robots = u.count.game >= 10
    ) {
      main(cls := "page-menu", dataUsername := u.username)(
        st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
        div(cls := "page-menu__content box user-show")(
          views.html.user.show.header(u, info, Angle.Activity, social),
          div(cls := "angle-content")(views.html.activity(u, activities))
        )
      )
    }

  def games(
      u: User,
      info: UserInfo,
      games: Paginator[Game],
      filters: lila.app.mashup.GameFilterMenu,
      searchForm: Option[Form[_]],
      social: lila.app.mashup.UserInfo.Social,
      notes: Map[Game.ID, String]
  )(implicit ctx: Context) =
    views.html.base.layout(
      title =
        s"${u.username} : ${userGameFilterTitleNoTag(u, info.nbs, filters.current)}${if (games.currentPage == 1) ""
        else " - page " + games.currentPage}",
      moreJs = moreJs(info, filters.current.name == "search"),
      moreCss = frag(
        cssTag("user.show"),
        filters.current.name == "search" option cssTag("user.show.search"),
        isGranted(_.UserModView) option cssTag("mod.user")
      ),
      robots = u.count.game >= 10
    ) {
      main(cls := "page-menu", dataUsername := u.username)(
        st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
        div(cls := "page-menu__content box user-show")(
          views.html.user.show.header(u, info, Angle.Games(searchForm), social),
          div(cls := "angle-content")(gamesContent(u, info.nbs, games, filters, filters.current.name, notes))
        )
      )
    }

  private def moreJs(info: UserInfo, withSearch: Boolean = false)(implicit ctx: Context) =
    frag(
      infiniteScrollTag,
      jsModule("user"),
      info.ratingChart.map { ratingChart =>
        frag(
          jsTag("chart/ratingHistory.js"),
          embedJsUnsafeLoadThen(s"lichess.ratingHistoryChart($ratingChart)")
        )
      },
      withSearch option jsModule("gameSearch"),
      isGranted(_.UserModView) option jsModule("mod.user")
    )

  def disabled(u: User)(implicit ctx: Context) =
    views.html.base.layout(title = u.username, robots = false) {
      main(cls := "box box-pad")(
        h1(u.username),
        p(trans.settings.thisAccountIsClosed())
      )
    }

  private val dataUsername = attr("data-username")
}
