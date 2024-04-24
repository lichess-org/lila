package views.html.user.show

import play.api.data.Form

import lila.app.mashup.{ UserInfo, GameFilter }
import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
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

  def userGameFilterTitle(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(using Translate): Frag =
    if filter == GameFilter.Search then frag(iconTag(Icon.Search), br, trans.search.advancedSearch())
    else splitNumber(userGameFilterTitleNoTag(u, nbs, filter))

  private def transLocalize(key: lila.core.i18n.I18nKey, number: Int)(using Translate) =
    key.pluralSameTxt(number)

  def userGameFilterTitleNoTag(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(using Translate): String =
    filter match
      case GameFilter.All      => transLocalize(trans.site.nbGames, u.count.game)
      case GameFilter.Me       => nbs.withMe.so { transLocalize(trans.site.nbGamesWithYou, _) }
      case GameFilter.Rated    => transLocalize(trans.site.nbRated, u.count.rated)
      case GameFilter.Win      => transLocalize(trans.site.nbWins, u.count.win)
      case GameFilter.Loss     => transLocalize(trans.site.nbLosses, u.count.loss)
      case GameFilter.Draw     => transLocalize(trans.site.nbDraws, u.count.draw)
      case GameFilter.Playing  => transLocalize(trans.site.nbPlaying, nbs.playing)
      case GameFilter.Bookmark => transLocalize(trans.site.nbBookmarks, nbs.bookmark)
      case GameFilter.Imported => transLocalize(trans.site.nbImportedGames, nbs.imported)
      case GameFilter.Search   => trans.search.advancedSearch.txt()

  private def describeUser(user: lila.core.perf.UserWithPerfs)(using Translate) =
    import lila.rating.UserPerfsExt.bestRatedPerf
    val name      = user.titleUsername
    val nbGames   = user.count.game
    val createdAt = dateHelper.showEnglishDate(user.createdAt)
    val currentRating = user.perfs.bestRatedPerf.so: p =>
      s" Current ${toNameKey(p.key).txt()} rating: ${p.perf.intRating}."
    s"$name played $nbGames games since $createdAt.$currentRating"

  private val i18nKeys = List(
    trans.site.youAreLeavingLichess,
    trans.site.neverTypeYourPassword,
    trans.site.cancel,
    trans.site.proceedToX
  )

  private val dataUsername = attr("data-username")
