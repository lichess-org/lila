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
        val fullDiffs = ("updatedAt" -> new Date()) :: diffs
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
    update(idSelector(id), $set(pn + ".userId" -> user.id, pn + ".elo" -> user.elo))
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
    val userIds = game.players.map(_.userId).flatten
    update(
      idSelector(game), 
      game.variant.standard.fold(
        $set("userIds" -> userIds),
        $set("userIds" -> userIds, "initialFen" -> (Forsyth >> game.toChess))
      )
    )
  }

  def saveInitialFen(game: DbGame): IO[Unit] = io {
    update(idSelector(game), $set("initialFen" -> (Forsyth >> game.toChess)))
  }

  def initialFen(gameId: String): IO[Option[String]] = io {
    primitiveProjection[String](idSelector(gameId), "initialFen")
  }

  def cleanupUnplayed: IO[Unit] = io {
    remove(("turns" $lt 2) ++ ("createdAt" $lt (DateTime.now - 2.day)))
  }

  def remove(gameId: String): IO[Unit] = io {
    remove(idSelector(gameId))
  }

  def candidatesToAutofinish: IO[List[DbGame]] = io {
    find(Query.playable ++
      Query.clock(true) ++
      ("updatedAt" $lt (DateTime.now - 2.hour))
    ).toList.map(_.decode).flatten
  }

  def count(query: DBObject): IO[Int] = io {
    super.count(query).toInt
  }

  def count(query: Query.type ⇒ DBObject): IO[Int] = count(query(Query))

  def exists(id: String) = count(idSelector(id)) map (_ > 0)

  def recentGames(limit: Int): IO[List[DbGame]] = io {
    find(Query.started)
      .sort(DBObject("updatedAt" -> -1))
      .limit(limit)
      .toList.map(_.decode).flatten sortBy (_.id)
  }

  def games(ids: List[String]): IO[List[DbGame]] = io {
    find("_id" $in ids).toList.map(_.decode).flatten sortBy (_.id)
  }

  private def idSelector(game: DbGame): DBObject = idSelector(game.id)
  private def idSelector(id: String): DBObject = DBObject("_id" -> id)
}
