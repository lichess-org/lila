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

  import GameFilter.*

  def apply(user: User, nbs: UserInfo.NbGames, currentName: String, isAuth: Boolean): GameFilterMenu =

    val filters: NonEmptyList[GameFilter] = NonEmptyList(
      All,
      List(
        (~nbs.withMe > 0).option(Me),
        (user.count.rated > 0).option(Rated),
        (user.count.win > 0).option(Win),
        (user.count.loss > 0).option(Loss),
        (user.count.draw > 0).option(Draw),
        (nbs.playing > 0).option(Playing),
        (nbs.bookmark > 0).option(Bookmark),
        (nbs.imported > 0).option(Imported),
        (isAuth && user.count.game > 0).option(Search)
      ).flatten
    )

    val current = currentOf(filters, currentName)

    lila.game.GameFilterMenu(filters, current)

  def currentOf(filters: NonEmptyList[GameFilter], name: String) =
    filters.find(_.name == name) | filters.head

  private def cachedNbOf(
      user: User,
      nbs: Option[UserInfo.NbGames],
      filter: GameFilter
  ): Option[Int] =
    filter match
      case Bookmark => nbs.map(_.bookmark)
      case Imported => nbs.map(_.imported)
      case All => user.count.game.some
      case Me => nbs.flatMap(_.withMe)
      case Rated => user.count.rated.some
      case Win => user.count.win.some
      case Loss => user.count.loss.some
      case Draw => user.count.draw.some
      case Search => user.count.game.some
      case Playing => nbs.map(_.playing)

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
        me: Option[User],
        page: Int
    )(using Request[?], FormBinding, Lang): Fu[Paginator[Game]] =
      val nb = cachedNbOf(user, nbs, filter)
      def std(query: Bdoc) = pagBuilder.recentlyCreated(query, nb)(page)
      filter match
        case Bookmark => bookmarkApi.gamePaginatorByUser(user, page)
        case Imported =>
          pagBuilder(
            selector = Query.imported(user.id),
            sort = $sort.desc("pgni.ca"),
            nb = nb
          )(page)
        case All =>
          std(Query.started(user.id)).flatMap:
            _.mapFutureResults(gameProxyRepo.upgradeIfPresent)
        case Me => std(Query.opponents(user, me | user))
        case Rated => std(Query.rated(user.id))
        case Win => std(Query.win(user.id))
        case Loss => std(Query.loss(user.id))
        case Draw => std(Query.draw(user.id))
        case Playing =>
          pagBuilder(
            selector = Query.nowPlaying(user.id),
            sort = $empty,
            nb = nb
          )(page)
            .flatMap:
              _.mapFutureResults(gameProxyRepo.upgradeIfPresent)
            .addEffect: p =>
              p.currentPageResults.filter(_.finishedOrAborted).foreach(gameRepo.unsetPlayingUids)
        case Search => userGameSearch(user, page)

  def searchForm(
      userGameSearch: lila.gameSearch.UserGameSearch,
      filter: GameFilter
  )(using Request[?], FormBinding, Lang): play.api.data.Form[?] =
    if filter == Search then userGameSearch.requestForm
    else userGameSearch.defaultForm
