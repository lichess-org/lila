package lila.app
package mashup

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.game.{ Game, Query }
import lila.user.User

import play.api.mvc.Request
import cats.data.NonEmptyList

sealed abstract class GameFilter(val name: String)

object GameFilter {
  case object All      extends GameFilter("all")
  case object Me       extends GameFilter("me")
  case object Rated    extends GameFilter("rated")
  case object Win      extends GameFilter("win")
  case object Loss     extends GameFilter("loss")
  case object Draw     extends GameFilter("draw")
  case object Playing  extends GameFilter("playing")
  case object Bookmark extends GameFilter("bookmark")
  case object Imported extends GameFilter("import")
  case object Search   extends GameFilter("search")
}

case class GameFilterMenu(
    all: NonEmptyList[GameFilter],
    current: GameFilter
) {

  def list = all.toList
}

object GameFilterMenu {

  import GameFilter._

  val all: NonEmptyList[GameFilter] =
    NonEmptyList.of(All, Me, Rated, Win, Loss, Draw, Playing, Bookmark, Imported, Search)

  def apply(user: User, nbs: UserInfo.NbGames, currentName: String, isAuth: Boolean): GameFilterMenu = {

    val filters: NonEmptyList[GameFilter] = NonEmptyList(
      All,
      List(
        (~nbs.withMe > 0) option Me,
        (user.count.rated > 0) option Rated,
        (user.count.win > 0) option Win,
        (user.count.loss > 0) option Loss,
        (user.count.draw > 0) option Draw,
        (nbs.playing > 0) option Playing,
        (nbs.bookmark > 0) option Bookmark,
        (nbs.imported > 0) option Imported,
        (isAuth && user.count.game > 0) option Search
      ).flatten
    )

    val current = currentOf(filters, currentName)

    new GameFilterMenu(filters, current)
  }

  def currentOf(filters: NonEmptyList[GameFilter], name: String) =
    filters.find(_.name == name) | filters.head

  private def cachedNbOf(
      user: User,
      nbs: Option[UserInfo.NbGames],
      filter: GameFilter
  ): Option[Int] =
    filter match {
      case Bookmark => nbs.map(_.bookmark)
      case Imported => nbs.map(_.imported)
      case All      => user.count.game.some
      case Me       => nbs.flatMap(_.withMe)
      case Rated    => user.count.rated.some
      case Win      => user.count.win.some
      case Loss     => user.count.loss.some
      case Draw     => user.count.draw.some
      case Search   => user.count.game.some
      case Playing  => nbs.map(_.playing)
      case _        => None
    }

  final class PaginatorBuilder(
      userGameSearch: lila.gameSearch.UserGameSearch,
      pagBuilder: lila.game.PaginatorBuilder,
      gameRepo: lila.game.GameRepo,
      gameProxyRepo: lila.round.GameProxyRepo,
      bookmarkApi: lila.bookmark.BookmarkApi
  )(implicit ec: scala.concurrent.ExecutionContext) {

    def apply(
        user: User,
        nbs: Option[UserInfo.NbGames],
        filter: GameFilter,
        me: Option[User],
        page: Int
    )(implicit req: Request[_]): Fu[Paginator[Game]] = {
      val nb               = cachedNbOf(user, nbs, filter)
      def std(query: Bdoc) = pagBuilder.recentlyCreated(query, nb)(page)
      filter match {
        case Bookmark => bookmarkApi.gamePaginatorByUser(user, page)
        case Imported =>
          pagBuilder(
            selector = Query imported user.id,
            sort = $sort desc "pgni.ca",
            nb = nb
          )(page)
        case All =>
          std(Query started user.id) flatMap {
            _.mapFutureResults(gameProxyRepo.upgradeIfPresent)
          }
        case Me    => std(Query.opponents(user, me | user))
        case Rated => std(Query rated user.id)
        case Win   => std(Query win user.id)
        case Loss  => std(Query loss user.id)
        case Draw  => std(Query draw user.id)
        case Playing =>
          pagBuilder(
            selector = Query nowPlaying user.id,
            sort = $empty,
            nb = nb
          )(page)
            .flatMap {
              _.mapFutureResults(gameProxyRepo.upgradeIfPresent)
            }
            .addEffect { p =>
              p.currentPageResults.filter(_.finishedOrAborted) foreach gameRepo.unsetPlayingUids
            }
        case Search => userGameSearch(user, page)
      }
    }
  }

  def searchForm(
      userGameSearch: lila.gameSearch.UserGameSearch,
      filter: GameFilter
  )(implicit req: Request[_]): play.api.data.Form[_] =
    filter match {
      case Search => userGameSearch.requestForm
      case _      => userGameSearch.defaultForm
    }
}
