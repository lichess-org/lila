package lila.app
package mashup

import play.api.data.FormBinding
import play.api.i18n.Lang
import play.api.mvc.Request
import scalalib.paginator.Paginator

import lila.core.game.Game
import lila.core.user.User
import lila.db.dsl.*
import lila.game.{ GameFilter, GameFilterMenu, Query }

object GameFilterMenu:

  def apply(user: User, nbs: UserInfo.NbGames, currentName: String, isAuth: Boolean): GameFilterMenu =

    val filters: NonEmptyList[GameFilter] = NonEmptyList(
      GameFilter.all,
      List(
        (~nbs.withMe > 0).option(GameFilter.me),
        (user.count.rated > 0).option(GameFilter.rated),
        (user.count.win > 0).option(GameFilter.win),
        (user.count.loss > 0).option(GameFilter.loss),
        (user.count.draw > 0).option(GameFilter.draw),
        (nbs.playing > 0).option(GameFilter.playing),
        (nbs.bookmark > 0).option(GameFilter.bookmark),
        (nbs.imported > 0).option(GameFilter.imported),
        (isAuth && user.count.game > 0).option(GameFilter.search)
      ).flatten
    )

    lila.game.GameFilterMenu(filters, GameFilter(currentName))

  private def cachedNbOf(
      user: User,
      nbs: Option[UserInfo.NbGames],
      filter: GameFilter
  ): Option[Int] =
    filter match
      case GameFilter.bookmark => nbs.map(_.bookmark)
      case GameFilter.imported => nbs.map(_.imported)
      case GameFilter.all => user.count.game.some
      case GameFilter.me => nbs.flatMap(_.withMe)
      case GameFilter.rated => user.count.rated.some
      case GameFilter.win => user.count.win.some
      case GameFilter.loss => user.count.loss.some
      case GameFilter.draw => user.count.draw.some
      case GameFilter.search => user.count.game.some
      case GameFilter.playing => nbs.map(_.playing)

  final class PaginatorBuilder(
      userGameSearch: lila.gameSearch.UserGameSearch,
      pagBuilder: lila.game.PaginatorBuilder,
      gameRepo: lila.game.GameRepo,
      gameProxyRepo: lila.round.GameProxyRepo,
      bookmarkApi: lila.bookmark.BookmarkApi
  )(using Executor):

    def apply(
        user: User,
        nbs: Option[UserInfo.NbGames],
        filter: GameFilter,
        page: Int
    )(using Request[?], FormBinding, Lang)(using meOpt: Option[Me]): Fu[Paginator[Game]] =
      val nb = cachedNbOf(user, nbs, filter)
      def std(query: Bdoc) = pagBuilder.recentlyCreated(query, nb)(page)
      filter match
        case GameFilter.bookmark => bookmarkApi.gamePaginatorByUser(user, page)
        case GameFilter.imported =>
          pagBuilder(
            selector = Query.imported(user.id),
            sort = $sort.desc("pgni.ca"),
            nb = nb
          )(page)
        case GameFilter.all =>
          std(Query.started(user.id)).flatMap:
            _.mapFutureResults(gameProxyRepo.upgradeIfPresent)
        case GameFilter.me => std(Query.opponents(user, meOpt.fold(user)(_.value)))
        case GameFilter.rated => std(Query.rated(user.id))
        case GameFilter.win => std(Query.win(user.id))
        case GameFilter.loss => std(Query.loss(user.id))
        case GameFilter.draw => std(Query.draw(user.id))
        case GameFilter.playing =>
          pagBuilder(
            selector = Query.nowPlaying(user.id),
            sort = $empty,
            nb = nb
          )(page)
            .flatMap:
              _.mapFutureResults(gameProxyRepo.upgradeIfPresent)
            .addEffect: p =>
              p.currentPageResults.filter(_.finishedOrAborted).foreach(gameRepo.unsetPlayingUids)
        case GameFilter.search => userGameSearch(user, page)

  def searchForm(
      userGameSearch: lila.gameSearch.UserGameSearch,
      filter: GameFilter
  )(using Request[?], FormBinding, Lang): play.api.data.Form[?] =
    if filter == GameFilter.search then userGameSearch.requestForm
    else userGameSearch.defaultForm
