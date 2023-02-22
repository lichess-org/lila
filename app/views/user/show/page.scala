package views.html.user.show

import controllers.routes
import play.api.data.Form

import lila.api.{ Context, given }
import lila.app.mashup.UserInfo
import lila.app.mashup.UserInfo.Angle
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.common.String.html.safeJsonValue
import lila.game.Game
import lila.user.User
import lila.history.RatingChartApi
import play.api.libs.json.Json

object page:

  def activity(
      u: User,
      activities: Vector[lila.activity.ActivityView],
      info: UserInfo,
      social: UserInfo.Social
  )(using Context) =
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
      searchForm: Option[Form[?]],
      social: UserInfo.Social,
      notes: Map[GameId, String]
  )(using Context) =
    val filterName = userGameFilterTitleNoTag(u, info.nbs, filters.current)
    val pageName   = (games.currentPage > 1) ?? s" - page ${games.currentPage}"
    views.html.base.layout(
      title = s"${u.username} $filterName$pageName",
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

  private def moreJs(info: UserInfo, withSearch: Boolean = false)(using Context) =
    frag(
      infiniteScrollTag,
      jsModule("user"),
      info.ratingChart.map { ratingChart =>
        frag(
          jsModule("chart.ratingHistory"),
          embedJsUnsafeLoadThen {
            s"LichessChartRatingHistory($ratingChart,{perfIndex:${RatingChartApi.bestPerfIndex(info.user)}})"
          }
        )
      },
      withSearch option jsModule("gameSearch"),
      isGranted(_.UserModView) option jsModule("mod.user"),
      embedJsUnsafeLoadThen(
        s"""UserProfile(${safeJsonValue(Json.obj("i18n" -> i18nJsObject(i18nKeys)))})"""
      )
    )

  def disabled(u: User)(using Context) =
    views.html.base.layout(title = u.username, robots = false) {
      main(cls := "box box-pad")(
        h1(cls := "box__top")(u.username),
        p(trans.settings.thisAccountIsClosed())
      )
    }

  private val i18nKeys = List(
    trans.youAreLeavingLichess,
    trans.neverTypeYourPassword,
    trans.cancel,
    trans.proceedToX
  )

  private val dataUsername = attr("data-username")
