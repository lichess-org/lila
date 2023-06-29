package lila.bookmark

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.game.{ Game, GameRepo }
import lila.user.User

case class Bookmark(game: Game, user: User)

final class BookmarkApi(
    coll: Coll,
    gameRepo: GameRepo,
    gameProxyRepo: lila.round.GameProxyRepo,
    paginator: PaginatorBuilder
)(using Executor):

  private def exists(gameId: GameId, userId: UserId): Fu[Boolean] =
    coll exists selectId(gameId, userId)

  def exists(game: Game, user: User): Fu[Boolean] =
    if game.bookmarks > 0 then exists(game.id, user.id)
    else fuFalse

  def exists(game: Game, user: Option[User]): Fu[Boolean] =
    user.so { exists(game, _) }

  def filterGameIdsBookmarkedBy(games: Seq[Game], user: Option[User]): Fu[Set[GameId]] =
    user.so: u =>
      val candidateIds = games collect { case g if g.bookmarks > 0 => g.id }
      candidateIds.nonEmpty so
        coll.secondaryPreferred
          .distinctEasy[GameId, Set]("g", userIdQuery(u.id) ++ $doc("g" $in candidateIds))

  def removeByGameId(gameId: GameId): Funit =
    coll.delete.one($doc("g" -> gameId)).void

  def removeByGameIds(gameIds: List[GameId]): Funit =
    coll.delete.one($doc("g" $in gameIds)).void

  def remove(gameId: GameId, userId: UserId): Funit = coll.delete.one(selectId(gameId, userId)).void

  def toggle(gameId: GameId, userId: UserId): Funit =
    exists(gameId, userId)
      .flatMap: e =>
        (if e then remove(gameId, userId) else add(gameId, userId, nowInstant)) inject !e
      .flatMap: bookmarked =>
        val inc = if bookmarked then 1 else -1
        gameRepo.incBookmarks(gameId, inc) >> gameProxyRepo.updateIfPresent(gameId)(_.incBookmarks(inc))
      .recover:
        lila.db.ignoreDuplicateKey

  def countByUser(user: User): Fu[Int] = coll.countSel(userIdQuery(user.id))

  def gamePaginatorByUser(user: User, page: Int) = paginator.byUser(user, page)

  private def add(gameId: GameId, userId: UserId, date: Instant): Funit =
    coll.insert
      .one:
        $doc(
          "_id" -> makeId(gameId, userId),
          "g"   -> gameId,
          "u"   -> userId,
          "d"   -> date
        )
      .void

  private def userIdQuery(userId: UserId)              = $doc("u" -> userId)
  private def makeId(gameId: GameId, userId: UserId)   = s"$gameId$userId"
  private def selectId(gameId: GameId, userId: UserId) = $id(makeId(gameId, userId))
