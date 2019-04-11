package views.html.user.show

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.mashup.UserInfo.Angle
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator
import lidraughts.game.Game
import lidraughts.user.User

import controllers.routes

object page {

  def activity(
    u: User,
    activities: Vector[lidraughts.activity.ActivityView],
    info: lidraughts.app.mashup.UserInfo,
    social: lidraughts.app.mashup.UserInfo.Social
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${u.username} : ${trans.activity.activity.txt()}",
    openGraph = lidraughts.app.ui.OpenGraph(
      image = staticUrl("images/large_tile.png").some,
      title = u.titleUsernameWithBestRating,
      url = s"$netBaseUrl${routes.User.show(u.username).url}",
      description = describeUser(u)
    ).some,
    moreJs = frag(
      jsAt("compiled/user.js"),
      info.ratingChart.map { ratingChart =>
        frag(
          jsTag("chart/ratingHistory.js"),
          embedJsUnsafe(s"lidraughts.ratingHistoryChart($ratingChart);")
        )
      },
      isGranted(_.UserSpy) option jsAt("compiled/user-mod.js")
    ),
    moreCss = frag(
      responsiveCssTag("user.show.activity"),
      isGranted(_.UserSpy) option responsiveCssTag("mod.user")
    )
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
    info: lidraughts.app.mashup.UserInfo,
    games: Paginator[Game],
    filters: lidraughts.app.mashup.GameFilterMenu,
    searchForm: Option[Form[_]],
    social: lidraughts.app.mashup.UserInfo.Social
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${u.username} : ${userGameFilterTitleNoTag(u, info.nbs, filters.current)}${if (games.currentPage == 1) "" else " - page " + games.currentPage}",
    moreJs = frag(
      infiniteScrollTag,
      jsAt("compiled/user.js"),
      info.ratingChart.map { ratingChart =>
        frag(
          jsTag("chart/ratingHistory.js"),
          embedJsUnsafe(s"lidraughts.ratingHistoryChart($ratingChart);"),
          filters.current.name == "search" option jsTag("search.js")
        )
      },
      isGranted(_.UserSpy) option jsAt("compiled/user-mod.js")
    ),
    moreCss = frag(
      responsiveCssTag("user.show.games"),
      info.nbs.crosstable.isDefined option responsiveCssTag("crosstable")
    )
  ) {
      main(cls := "page-menu", dataUsername := u.username)(
        st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
        div(cls := "page-menu__content box user-show")(
          header(u, info, Angle.Games(searchForm), social),
          div(cls := "angle-content")(gamesContent(u, info.nbs, games, filters, filters.current.name))
        )
      )
    }

  def disabled(u: User)(implicit ctx: Context) =
    views.html.base.layout(title = u.username, robots = false) {
      main(cls := "box box-pad")(
        h1(u.username),
        p(trans.thisAccountIsClosed.frag())
      )
    }

  private val dataUsername = attr("data-username")
}
