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
    update(DBObject("_id" -> game.id), _grater asDBObject game.encode)
  }

  def save(progress: Progress): IO[Unit] =
    new GameDiff(progress.origin.encode, progress.game.encode)() |> { diffs ⇒
      if (diffs.nonEmpty) {
        val fullDiffs = ("updatedAt" -> new Date()) :: diffs
        io { update(DBObject("_id" -> progress.origin.id), $set(fullDiffs: _*)) }
      }
      else io()
    }

  def insert(game: DbGame): IO[Option[String]] = io {
    insert(game.encode)
  }

  // makes the asumption that player 0 is white!
  // proved to be true on prod DB at March 31 2012
  def setEloDiffs(id: String, white: Int, black: Int) = io {
    update(
      DBObject("_id" -> id),
      $set("players.0.eloDiff" -> white, "players.1.eloDiff" -> black)
    )
  }

  def setUser(id: String, color: Color, dbRef: DBRef, elo: Int) = io {
    val playerName = "players.%d".format(color.fold(0, 1))
    update(
      DBObject("_id" -> id),
      $set(playerName + ".user" -> dbRef, playerName + ".elo" -> elo)
    )
  }

  def finish(id: String, winnerId: Option[String]) = io {
    update(
      DBObject("_id" -> id),
      winnerId.fold(userId ⇒
        $set("positionHashes" -> "", "winnerUserId" -> userId),
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

  val findOneStandardCheckmate: IO[Option[DbGame]] = io {
    find(DBObject(
      "status" -> Status.Mate.id,
      "v" -> Variant.Standard.id
    ))
      .sort(DBObject("createdAt" -> -1))
      .limit(1)
      .toList.map(_.decode).flatten.headOption
  }

  def saveInitialFen(dbGame: DbGame): IO[Unit] = io {
    update(
      DBObject("_id" -> dbGame.id),
      $set("initialFen" -> (Forsyth >> dbGame.toChess))
    )
  }

  def initialFen(gameId: String): IO[Option[String]] = io {
    primitiveProjection[String](DBObject("_id" -> gameId), "initialFen")
  }

  def cleanupUnplayed: IO[Unit] = io {
    remove(("turns" $lt 2) ++ ("createdAt" $lt (DateTime.now - 2.day)))
  }

  def remove(gameId: String): IO[Unit] = io {
    remove(DBObject("_id" -> gameId))
  }

  def candidatesToAutofinish: IO[List[DbGame]] = io {
    find(
      ("clock.l" $exists true) ++
        ("status" -> Status.Started.id) ++
        ("updatedAt" $lt (DateTime.now - 2.hour))
    ).toList.map(_.decode).flatten
  }

  val countAll: IO[Int] = io { count().toInt }

  val countPlaying: IO[Int] = io {
    count("updatedAt" $gt (DateTime.now - 15.seconds)).toInt
  }

  val countMate: IO[Int] = io {
    count(DBObject("status" -> Status.Mate.id)).toInt
  }

  def countWinBy(user: User): IO[Int] = io {
    count(DBObject("winnerUserId" -> user.id.toString)).toInt
  }

  def countDrawBy(user: User): IO[Int] = io {
    count(
      ("status" $in List(Status.Draw.id, Status.Stalemate.id)) ++ 
      ("userIds" -> user.id.toString)
    ).toInt
  }

  def countLossBy(user: User): IO[Int] = io {
    count(
      ("status" $in List(Status.Mate.id, Status.Resign.id, Status.Outoftime.id, Status.Timeout.id)) ++ 
      ("userIds" -> user.id.toString) ++
      ("winnerUserId" $ne user.id.toString)
    ).toInt
  }

  def recentGames(limit: Int): IO[List[DbGame]] = io {
    find(DBObject("status" -> Status.Started.id))
      .sort(DBObject("updatedAt" -> -1))
      .limit(limit)
      .toList.map(_.decode).flatten sortBy (_.id)
  }

  def games(ids: List[String]): IO[List[DbGame]] = io {
    find("_id" $in ids).toList.map(_.decode).flatten sortBy (_.id)
  }

  def ensureIndexes: IO[Unit] = io {
    collection.underlying |> { coll ⇒
      coll.ensureIndex(DBObject("status" -> 1))
      coll.ensureIndex(DBObject("userIds" -> 1))
      coll.ensureIndex(DBObject("winnerUserId" -> 1))
      coll.ensureIndex(DBObject("turns" -> 1))
      coll.ensureIndex(DBObject("updatedAt" -> -1))
      coll.ensureIndex(DBObject("createdAt" -> -1))
      coll.ensureIndex(DBObject("createdAt" -> -1, "userIds" -> 1))
    }
  }

  def dropIndexes: IO[Unit] = io {
    collection.dropIndexes()
  }
}
