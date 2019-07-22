package views.html.user.show

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.mashup.UserInfo.Angle
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator
import lidraughts.app.mashup.UserInfo
import lidraughts.game.Game
import lidraughts.user.User

import controllers.routes

object page {

  def activity(
    u: User,
    activities: Vector[lidraughts.activity.ActivityView],
    info: UserInfo,
    social: UserInfo.Social
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${u.username} : ${trans.activity.activity.txt()}",
    openGraph = lidraughts.app.ui.OpenGraph(
      image = staticUrl("images/large_tile.png").some,
      title = u.titleUsernameWithBestRating,
      url = s"$netBaseUrl${routes.User.show(u.username).url}",
      description = describeUser(u)
    ).some,
    moreJs = moreJs(info),
    moreCss = frag(
      cssTag("user.show"),
      isGranted(_.UserSpy) option cssTag("mod.user")
    ),
    robots = u.count.game >= 10
  ) {
      main(cls := "page-menu", dataUsername := u.username)(
        st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
        div(cls := "page-menu__content box user-show")(
          header(u, info, Angle.Activity, social),
          div(cls := "angle-content")(views.html.activity(u, activities))
        )
      )
    }

  def games(
    u: User,
    info: UserInfo,
    games: Paginator[Game],
    filters: lidraughts.app.mashup.GameFilterMenu,
    searchForm: Option[Form[_]],
    social: lidraughts.app.mashup.UserInfo.Social
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${u.username} : ${userGameFilterTitleNoTag(u, info.nbs, filters.current)}${if (games.currentPage == 1) "" else " - page " + games.currentPage}",
    moreJs = moreJs(info, filters.current.name == "search"),
    moreCss = frag(
      cssTag("user.show"),
      filters.current.name == "search" option cssTag("user.show.search"),
      isGranted(_.UserSpy) option cssTag("mod.user")
    ),
    robots = u.count.game >= 10
  ) {
      main(cls := "page-menu", dataUsername := u.username)(
        st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
        div(cls := "page-menu__content box user-show")(
          header(u, info, Angle.Games(searchForm), social),
          div(cls := "angle-content")(gamesContent(u, info.nbs, games, filters, filters.current.name))
        )
      )
    }

  private def moreJs(info: UserInfo, withSearch: Boolean = false)(implicit ctx: Context) = frag(
    infiniteScrollTag,
    jsAt("compiled/user.js"),
    info.ratingChart.map { ratingChart =>
      frag(
        jsTag("chart/ratingHistory.js"),
        embedJsUnsafe(s"lidraughts.ratingHistoryChart($ratingChart);")
      )
    },
    withSearch option frag(jsTag("search.js")),
    isGranted(_.UserSpy) option jsAt("compiled/user-mod.js")
  )

  def disabled(u: User)(implicit ctx: Context) =
    views.html.base.layout(title = u.username, robots = false) {
      main(cls := "box box-pad")(
        h1(u.username),
        p(trans.thisAccountIsClosed())
      )
    }

  private val dataUsername = attr("data-username")
}
