package lila.bookmark

import reactivemongo.api.bson.*

import lila.core.game.{ Game, GameApi }
import lila.db.dsl.{ *, given }
import chess.Ply
import chess.format.SimpleFen
import chess.format.Uci
import scala.util.Failure

case class Bookmark(game: Game, position: Option[BookmarkPosition] = None)
case class BookmarkPosition(ply: Ply, fen: SimpleFen, color: Color, lastMove: Option[Uci])

object BookmarkPosition:
  def apply(
      ply: Option[String],
      fen: Option[String],
      color: Option[String],
      lastMoveUci: Option[String]
  ): Option[BookmarkPosition] =
    for
      p <- ply.flatMap(_.toIntOption.map(Ply(_)))
      f <- fen.map(SimpleFen(_))
      c <- color.flatMap(c => Color(c.head))
      u = lastMoveUci.flatMap(Uci(_))
    yield BookmarkPosition(p, f, c, u)

given BSONWriter[Uci] = BSONWriter { uci => BSONString(uci.uci) }
given BSONReader[Uci] = BSONReader.collect { case BSONString(str) => Uci(str).get }

given BSONDocumentHandler[BookmarkPosition] = Macros.handler[BookmarkPosition]

final class BookmarkApi(val coll: Coll, gameApi: GameApi, paginator: PaginatorBuilder)(using Executor):

  private def exists(gameId: GameId, userId: UserId): Fu[Boolean] =
    coll.exists(selectId(gameId, userId))

  def exists(game: Game, userId: UserId): Fu[Boolean] =
    (game.bookmarks > 0).so(exists(game.id, userId))

  def exists(game: Game, user: Option[UserId]): Fu[Boolean] =
    user.so { exists(game, _) }

  def filterGameIdsBookmarkedBy(games: Seq[Game], user: Option[User]): Fu[Set[GameId]] =
    user.so: u =>
      val candidateIds = games.collect { case g if g.bookmarks > 0 => g.id }
      candidateIds.nonEmpty.so:
        coll.secondary
          .distinctEasy[GameId, Set]("g", userIdQuery(u.id) ++ $doc("g".$in(candidateIds)))

  def removeByGameId(gameId: GameId): Funit =
    coll.delete.one($doc("g" -> gameId)).void

  def removeByGameIds(gameIds: List[GameId]): Funit =
    coll.delete.one($doc("g".$in(gameIds))).void

  def remove(gameId: GameId, userId: UserId): Funit = coll.delete.one(selectId(gameId, userId)).void

  def toggle(
      updateProxy: GameId => Update[Game] => Funit
  )(
      gameId: GameId,
      userId: UserId,
      v: Option[Boolean],
      position: Option[BookmarkPosition]
  ): Funit =
    exists(gameId, userId)
      .flatMap: e =>
        val newValue = v.getOrElse(!e)
        if e == newValue then funit
        else
          for
            _ <-
              if newValue then add(gameId, userId, nowInstant, position) else remove(gameId, userId)
            inc = if newValue then 1 else -1
            _ <- gameApi.incBookmarks(gameId, inc)
            _ <- updateProxy(gameId)(g => g.copy(bookmarks = g.bookmarks + inc))
          yield ()
      .recover:
        lila.db.ignoreDuplicateKey

  def countByUser(user: User): Fu[Int] = coll.countSel(userIdQuery(user.id))

  def gamePaginatorByUser(user: User, page: Int) = paginator.byUser(user, page)

  private def add(
      gameId: GameId,
      userId: UserId,
      date: Instant,
      position: Option[BookmarkPosition]
  ): Funit =
    coll.insert
      .one:
        $doc(
          "_id" -> makeId(gameId, userId),
          "g" -> gameId,
          "u" -> userId,
          "p" -> position,
          "d" -> date
        )
      .void

  def bookmarks(games: Seq[Game])(using me: MyId): Fu[Map[GameId, Bookmark]] =
    (coll
      .byIds(games.map(game => makeId(game.id, me)), _.sec): Fu[List[BSONDocument]])
      .map { docs =>
        (for
          doc <- docs
          gameId <- doc.getAsOpt[GameId]("g")
          position = doc.getAsOpt[BookmarkPosition]("p")
          game <- games.find(_.id == gameId)
        yield (gameId, Bookmark(game, position))).toMap
      }

  def userIdQuery(userId: UserId) = $doc("u" -> userId)
  private def makeId(gameId: GameId, userId: UserId) = s"$gameId$userId"
  private def selectId(gameId: GameId, userId: UserId) = $id(makeId(gameId, userId))

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    coll.delete.one(userIdQuery(del.id)).void
