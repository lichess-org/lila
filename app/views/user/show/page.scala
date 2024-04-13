package views.html.user.show

import controllers.routes
import play.api.data.Form

import lila.app.mashup.UserInfo
import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }
import lila.game.Game

import lila.core.data.SafeJsonStr
import lila.rating.UserWithPerfs.titleUsernameWithBestRating

object page:

  def activity(
      activities: Vector[lila.activity.ActivityView],
      info: UserInfo,
      social: UserInfo.Social
  )(using PageContext) =
    val u = info.user
    views.html.base.layout(
      title = s"${u.username} : ${trans.activity.activity.txt()}",
      openGraph = lila.web
        .OpenGraph(
          image = assetUrl("logo/lichess-tile-wide.png").some,
          twitterImage = assetUrl("logo/lichess-tile.png").some,
          title = u.titleUsernameWithBestRating,
          url = s"$netBaseUrl${routes.User.show(u.username).url}",
          description = describeUser(u)
        )
        .some,
      pageModule = pageModule(info),
      modules = esModules(info),
      moreCss = frag(
        cssTag("user.show"),
        isGranted(_.UserModView).option(cssTag("mod.user"))
      ),
      robots = u.count.game >= 10
    ):
      main(cls := "page-menu", dataUsername := u.username)(
        st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
        div(cls := "page-menu__content box user-show")(
          views.html.user.show.header(u, info, UserInfo.Angle.Activity, social),
          div(cls := "angle-content")(views.html.activity(u, activities))
        )
      )

  def games(
      info: UserInfo,
      games: scalalib.paginator.Paginator[Game],
      filters: lila.app.mashup.GameFilterMenu,
      searchForm: Option[Form[?]],
      social: UserInfo.Social,
      notes: Map[GameId, String]
  )(using PageContext) =
    val u          = info.user
    val filterName = userGameFilterTitleNoTag(u, info.nbs, filters.current)
    val pageName   = (games.currentPage > 1).so(s" - page ${games.currentPage}")
    views.html.base.layout(
      title = s"${u.username} $filterName$pageName",
      pageModule = pageModule(info),
      modules = esModules(info, filters.current.name == "search"),
      moreCss = frag(
        cssTag("user.show"),
        (filters.current.name == "search").option(cssTag("user.show.search")),
        isGranted(_.UserModView).option(cssTag("mod.user"))
      ),
      robots = u.count.game >= 10
    ) {
      main(cls := "page-menu", dataUsername := u.username)(
        st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
        div(cls := "page-menu__content box user-show")(
          views.html.user.show.header(u, info, UserInfo.Angle.Games(searchForm), social),
          div(cls := "angle-content")(gamesContent(u, info.nbs, games, filters, filters.current.name, notes))
        )
      )
    }

  private def esModules(info: UserInfo, withSearch: Boolean = false)(using PageContext): EsmList =
    import play.api.libs.json.Json
    infiniteScrollTag
      ++ jsModuleInit("bits.user", Json.obj("i18n" -> i18nJsObject(i18nKeys)))
      ++ withSearch.so(jsModule("bits.gameSearch"))
      ++ isGranted(_.UserModView).so(jsModule("mod.user"))

  private def pageModule(info: UserInfo)(using PageContext) =
    info.ratingChart.map: rc =>
      PageModule("chart.ratingHistory", SafeJsonStr(s"""{"data":$rc}"""))

  def disabled(u: User)(using PageContext) =
    views.html.base.layout(title = u.username, robots = false):
      main(cls := "box box-pad")(
        h1(cls := "box__top")(u.username),
        p(trans.settings.thisAccountIsClosed())
      )

  private val i18nKeys = List(
    trans.site.youAreLeavingLichess,
    trans.site.neverTypeYourPassword,
    trans.site.cancel,
    trans.site.proceedToX
  )

  private val dataUsername = attr("data-username")
