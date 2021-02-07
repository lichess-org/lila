package lila.bookmark

import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.game.{ Game, GameRepo }
import lila.user.User

case class Bookmark(game: lila.game.Game, user: lila.user.User)

final class BookmarkApi(
    coll: Coll,
    gameRepo: GameRepo,
    paginator: PaginatorBuilder
)(implicit ec: scala.concurrent.ExecutionContext) {

  private def exists(gameId: Game.ID, userId: User.ID): Fu[Boolean] =
    coll exists selectId(gameId, userId)

  def exists(game: Game, user: User): Fu[Boolean] =
    if (game.bookmarks > 0) exists(game.id, user.id)
    else fuFalse

  def exists(game: Game, user: Option[User]): Fu[Boolean] =
    user.?? { exists(game, _) }

  def filterGameIdsBookmarkedBy(games: Seq[Game], user: Option[User]): Fu[Set[Game.ID]] =
    user ?? { u =>
      val candidateIds = games collect { case g if g.bookmarks > 0 => g.id }
      candidateIds.nonEmpty ??
        coll.secondaryPreferred
          .distinctEasy[Game.ID, Set]("g", userIdQuery(u.id) ++ $doc("g" $in candidateIds))
    }

  def removeByGameId(gameId: Game.ID): Funit =
    coll.delete.one($doc("g" -> gameId)).void

  def removeByGameIds(gameIds: List[Game.ID]): Funit =
    coll.delete.one($doc("g" $in gameIds)).void

  def remove(gameId: Game.ID, userId: User.ID): Funit = coll.delete.one(selectId(gameId, userId)).void

  def toggle(gameId: Game.ID, userId: User.ID): Funit =
    exists(gameId, userId) flatMap { e =>
      (if (e) remove(gameId, userId) else add(gameId, userId, DateTime.now)) inject !e
    } flatMap { bookmarked =>
      gameRepo.incBookmarks(gameId, if (bookmarked) 1 else -1)
    }

  def countByUser(user: User): Fu[Int] = coll.countSel(userIdQuery(user.id))

  def gamePaginatorByUser(user: User, page: Int) = paginator.byUser(user, page)

  private def add(gameId: Game.ID, userId: User.ID, date: DateTime): Funit =
    coll.insert
      .one(
        $doc(
          "_id" -> makeId(gameId, userId),
          "g"   -> gameId,
          "u"   -> userId,
          "d"   -> date
        )
      )
      .void

  private def userIdQuery(userId: User.ID)               = $doc("u" -> userId)
  private def makeId(gameId: Game.ID, userId: User.ID)   = s"$gameId$userId"
  private def selectId(gameId: Game.ID, userId: User.ID) = $id(makeId(gameId, userId))
}
