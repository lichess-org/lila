package views.user
package show

import play.api.data.Form

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.UserInfo
import lila.core.data.SafeJsonStr
import lila.game.GameFilter
import lila.rating.UserWithPerfs.titleUsernameWithBestRating
import chess.Ply
import chess.format.SimpleFen

lazy val ui = lila.user.ui.UserShow(helpers, bits)

object page:

  lazy val side = lila.user.ui.UserShowSide(helpers)

  private def indexable(u: User) = u.isVerified || u.count.game >= 10

  def activity(
      activities: Seq[lila.activity.ActivityView],
      info: UserInfo,
      social: UserInfo.Social
  )(using Context) =
    val u = info.user
    Page(s"${u.username} : ${trans.activity.activity.txt()}")
      .graph(
        OpenGraph(
          image = staticAssetUrl("logo/lichess-tile-wide.png").some,
          title = u.titleUsernameWithBestRating,
          url = routeUrl(routes.User.show(u.username)),
          description = ui.describeUser(u)
        )
      )
      .js(pageModule(info))
      .js(esModules())
      .js(isGranted(_.UserModView).option(esmInit("mod.autolink")))
      .css("user.show")
      .css(isGranted(_.UserModView).option("mod.user"))
      .flag(_.noRobots, !indexable(u)):
        main(cls := "page-menu", ui.dataUsername := u.username)(
          st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
          div(cls := "page-menu__content box user-show")(
            views.user.show.header(u, info, UserInfo.Angle.Activity, social),
            div(cls := "angle-content")(views.activity(u, activities))
          )
        )

  def games(
      info: UserInfo,
      games: scalalib.paginator.Paginator[Game],
      filters: lila.game.GameFilterMenu,
      searchForm: Option[Form[?]],
      social: UserInfo.Social,
      notes: Map[GameId, String],
      plysAndFens: Map[GameId, (Ply, SimpleFen, String)]
  )(using Context) =
    val u = info.user
    val filterName = userGameFilterTitleNoTag(u, info.nbs, filters.current)
    val pageName = (games.currentPage > 1).so(s" - page ${games.currentPage}")
    plysAndFens.foreach(pf => lila.log("RRR").info(s"${pf._1}    ${pf._2}"))
    Page(s"${u.username} $filterName$pageName")
      .js(pageModule(info))
      .js(esModules(filters.current.name == "search"))
      .css("user.show")
      .css((filters.current.name == "search").option("user.show.search"))
      .css(isGranted(_.UserModView).option("mod.user"))
      .flag(_.noRobots, !indexable(u)):
        main(cls := "page-menu", ui.dataUsername := u.username)(
          st.aside(cls := "page-menu__menu")(side(u, info.ranks, none)),
          div(cls := "page-menu__content box user-show")(
            views.user.show.header(u, info, UserInfo.Angle.Games(searchForm), social),
            div(cls := "angle-content")(
              gamesContent(u, info.nbs, games, filters, filters.current.name, notes, plysAndFens)
            )
          )
        )

  private def esModules(withSearch: Boolean = false)(using Context): EsmList =
    infiniteScrollEsmInit
      ++ esmInit("user")
      ++ Esm("bits.dropdownOverflow")
      ++ withSearch.so(Esm("bits.gameSearch"))
      ++ isGranted(_.UserModView).so(Esm("mod.user"))

  private def pageModule(info: UserInfo): Option[PageModule] =
    info.ratingChart.map: rc =>
      PageModule("chart.ratingHistory", SafeJsonStr(s"""{"data":$rc}"""))

  def deleted(canCreate: Boolean) =
    Page("No such player"):
      main(cls := "page-small box box-pad page")(
        h1(cls := "box__top")("No such player"),
        div(
          p("This username doesn't match any Lichess player."),
          canCreate.not.option(p("It cannot be used to create a new account."))
        )
      )

  def userGameFilterTitle(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(using
      Context,
      Translate
  ): Frag =
    if filter == GameFilter.search then frag(iconTag(Icon.Search), br, trans.search.advancedSearch())
    else lila.web.ui.bits.splitNumber(userGameFilterTitleNoTag(u, nbs, filter))

  def userGameFilterTitleNoTag(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(using Translate): String =
    import ui.transLocalize
    filter match
      case GameFilter.all => transLocalize(trans.site.nbGames, u.count.game)
      case GameFilter.me => nbs.withMe.so { transLocalize(trans.site.nbGamesWithYou, _) }
      case GameFilter.rated => transLocalize(trans.site.nbRated, u.count.rated)
      case GameFilter.win => transLocalize(trans.site.nbWins, u.count.win)
      case GameFilter.loss => transLocalize(trans.site.nbLosses, u.count.loss)
      case GameFilter.draw => transLocalize(trans.site.nbDraws, u.count.draw)
      case GameFilter.playing => transLocalize(trans.site.nbPlaying, nbs.playing)
      case GameFilter.bookmark => transLocalize(trans.site.nbBookmarks, nbs.bookmark)
      case GameFilter.imported => transLocalize(trans.site.nbImportedGames, nbs.imported)
      case GameFilter.search => trans.search.advancedSearch.txt()
