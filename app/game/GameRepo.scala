package lila
package game

import DbGame._

import chess.{ Color, Variant, Status }
import chess.format.Forsyth
import round.Progress
import user.User

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import java.util.Date
import scala.util.Random
import scalaz.effects._

class GameRepo(collection: MongoCollection)
    extends SalatDAO[RawDbGame, String](collection) {

  def game(gameId: String): IO[Option[DbGame]] = io {
    if (gameId.size != gameIdSize) None
    else findOneByID(gameId) flatMap (_.decode)
  }

  def player(gameId: String, color: Color): IO[Option[DbPlayer]] =
    game(gameId) map { gameOption ⇒
      gameOption map { _ player color }
    }

  def pov(gameId: String, color: Color): IO[Option[Pov]] =
    game(gameId) map { gameOption ⇒
      gameOption map { g ⇒ Pov(g, g player color) }
    }

  def pov(gameId: String, color: String): IO[Option[Pov]] =
    Color(color).fold(pov(gameId, _), io(None))

  def pov(fullId: String): IO[Option[Pov]] =
    game(fullId take gameIdSize) map { gameOption ⇒
      gameOption flatMap { g ⇒
        g player (fullId drop gameIdSize) map { Pov(g, _) }
      }
    }

  def pov(ref: PovRef): IO[Option[Pov]] = pov(ref.gameId, ref.color)

  def save(game: DbGame): IO[Unit] = io {
    update(idSelector(game), _grater asDBObject game.encode)
  }

  def save(progress: Progress): IO[Unit] =
    new GameDiff(progress.origin.encode, progress.game.encode)() |> { diffs ⇒
      if (diffs.nonEmpty) {
        val fullDiffs = ("updatedAt" -> new Date) :: diffs
        io { update(idSelector(progress.origin), $set(fullDiffs: _*)) }
      }
      else io()
    }

  def insert(game: DbGame): IO[Option[String]] = io {
    insert(game.encode)
  }

  // makes the asumption that player 0 is white!
  // proved to be true on prod DB at March 31 2012
  def setEloDiffs(id: String, white: Int, black: Int) = io {
    update(idSelector(id), $set("players.0.ed" -> white, "players.1.ed" -> black))
  }

  def setUser(id: String, color: Color, user: User) = io {
    val pn = "players.%d".format(color.fold(0, 1))
    update(idSelector(id), $set(pn + ".uid" -> user.id, pn + ".elo" -> user.elo))
  }

  def incBookmarks(id: String, value: Int) = io {
    update(idSelector(id), $inc("bm" -> value))
  }

  def finish(id: String, winnerId: Option[String]) = io {
    update(
      idSelector(id),
      winnerId.fold(userId ⇒
        $set("positionHashes" -> "", "winId" -> userId),
        $set("positionHashes" -> ""))
        ++ $unset(
          "players.0.previousMoveTs",
          "players.1.previousMoveTs",
          "players.0.lastDrawOffer",
          "players.1.lastDrawOffer",
          "players.0.isOfferingDraw",
          "players.1.isOfferingDraw",
          "players.0.isProposingTakeback",
          "players.1.isProposingTakeback"
        )
    )
  }

  def findRandomStandardCheckmate(distribution: Int): IO[Option[DbGame]] = io {
    find(DBObject(
      "status" -> Status.Mate.id,
      "v" -> Variant.Standard.id
    ))
      .sort(DBObject("createdAt" -> -1))
      .limit(1)
      .skip(Random nextInt distribution)
      .toList.map(_.decode).flatten.headOption
  }

  def denormalizeStarted(game: DbGame): IO[Unit] = io {
    update(idSelector(game),
      $set("userIds" -> game.players.map(_.userId).flatten))
    update(idSelector(game), game.mode.rated.fold(
      $set("isRated" -> true), $unset("isRated")))
    if (game.variant.exotic) update(idSelector(game),
      $set("initialFen" -> (Forsyth >> game.toChess)))
  }

  def saveNext(game: DbGame, nextId: String): IO[Unit] = io {
    update(
      idSelector(game),
      $set("next" -> nextId) ++
        $unset("players.0.isOfferingRematch", "players.1.isOfferingRematch")
    )
  }

  def initialFen(gameId: String): IO[Option[String]] = io {
    primitiveProjection[String](idSelector(gameId), "initialFen")
  }

  val unplayedIds: IO[List[String]] = io {
    primitiveProjections[String](
      ("turns" $lt 2) ++ ("createdAt" $lt (DateTime.now - 2.day)),
      "_id"
    )
  }

  // bookmarks should also be removed
  def remove(id: String): IO[Unit] = io {
    remove(idSelector(id))
  }

  def removeIds(ids: List[String]): IO[Unit] = io {
    remove("_id" $in ids)
  }

  def candidatesToAutofinish: IO[List[DbGame]] = io {
    find(Query.playable ++
      Query.clock(true) ++
      ("createdAt" $gt (DateTime.now - 1.day)) ++ // index
      ("updatedAt" $lt (DateTime.now - 2.hour))
    ).toList.map(_.decode).flatten
  }

  def featuredCandidates: IO[List[DbGame]] = io {
    find(Query.playable ++
      Query.clock(true) ++
      ("turns" $gt 1) ++
      ("createdAt" $gt (DateTime.now - 3.minutes))
    ).toList.map(_.decode).flatten
  }

  def count(query: DBObject): IO[Int] = io {
    super.count(query).toInt
  }

  def count(query: Query.type ⇒ DBObject): IO[Int] = count(query(Query))

  def exists(id: String) = count(idSelector(id)) map (_ > 0)

  def recentGames(limit: Int): IO[List[DbGame]] = io {
    find(Query.started ++ Query.turnsGt(1))
      .sort(DBObject("createdAt" -> -1))
      .limit(limit)
      .toList.map(_.decode).flatten sortBy (_.id)
  }

  def games(ids: List[String]): IO[List[DbGame]] = io {
    find("_id" $in ids).toList.map(_.decode).flatten sortBy (_.id)
  }

  private def idSelector(game: DbGame): DBObject = idSelector(game.id)
  private def idSelector(id: String): DBObject = DBObject("_id" -> id)
}
