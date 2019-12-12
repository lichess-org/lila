package lila.bookmark

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._
import lila.game.{ Game, GameRepo }
import lila.user.User

case class Bookmark(game: lila.game.Game, user: lila.user.User)

final class BookmarkApi(
    coll: Coll,
    paginator: PaginatorBuilder
) {

  private def exists(gameId: String, userId: User.ID): Fu[Boolean] =
    coll exists selectId(gameId, userId)

  def exists(game: Game, user: User): Fu[Boolean] =
    if (game.bookmarks > 0) exists(game.id, user.id)
    else fuFalse

  def exists(game: Game, user: Option[User]): Fu[Boolean] =
    user.?? { exists(game, _) }

  def filterGameIdsBookmarkedBy(games: Seq[Game], user: Option[User]): Fu[Set[String]] =
    user ?? { u =>
      val candidateIds = games.filter(_.bookmarks > 0).map(_.id)
      if (candidateIds.isEmpty) fuccess(Set.empty)
      else coll.distinct[String, Set]("g", Some(
        userIdQuery(u.id) ++ $doc("g" $in candidateIds)
      ))
    }

  def removeByGameId(gameId: String): Funit =
    coll.remove($doc("g" -> gameId)).void

  def removeByGameIds(gameIds: List[String]): Funit =
    coll.remove($doc("g" $in gameIds)).void

  def remove(gameId: String, userId: User.ID): Funit = coll.remove(selectId(gameId, userId)).void
  // def remove(selector: Bdoc): Funit = coll.remove(selector).void

  def toggle(gameId: String, userId: User.ID): Funit =
    exists(gameId, userId) flatMap { e =>
      (if (e) remove(gameId, userId) else add(gameId, userId, DateTime.now)) inject !e
    } flatMap { bookmarked =>
      GameRepo.incBookmarks(gameId, if (bookmarked) 1 else -1)
    }

  def countByUser(user: User): Fu[Int] = coll.countSel(userIdQuery(user.id))

  def gamePaginatorByUser(user: User, page: Int) =
    paginator.byUser(user, page) map2 { (b: Bookmark) => b.game }

  private def add(gameId: String, userId: User.ID, date: DateTime): Funit =
    coll.insert($doc(
      "_id" -> makeId(gameId, userId),
      "g" -> gameId,
      "u" -> userId,
      "d" -> date
    )).void

  private def userIdQuery(userId: User.ID) = $doc("u" -> userId)
  private def makeId(gameId: String, userId: User.ID) = s"$gameId$userId"
  private def selectId(gameId: String, userId: User.ID) = $id(makeId(gameId, userId))
}
