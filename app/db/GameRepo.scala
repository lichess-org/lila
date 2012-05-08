package lila
package db

import model._
import DbGame._

import chess.Color
import chess.format.Forsyth

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
    else findOneByID(gameId) flatMap decode
  }

  def player(gameId: String, color: Color): IO[Option[DbPlayer]] =
    game(gameId) map { gameOption ⇒
      gameOption map { _ player color }
    }

  def pov(gameId: String, color: Color): IO[Option[Pov]] =
    game(gameId) map { gameOption ⇒
      gameOption map { g ⇒ Pov(g, g player color) }
    }

  def pov(fullId: String): IO[Option[Pov]] =
    game(fullId take gameIdSize) map { gameOption ⇒
      gameOption flatMap { g ⇒
        g player (fullId drop gameIdSize) map { Pov(g, _) }
      }
    }

  def pov(ref: PovRef): IO[Option[Pov]] = pov(ref.gameId, ref.color)

  def save(game: DbGame): IO[Unit] = io {
    update(DBObject("_id" -> game.id), _grater asDBObject encode(game))
  }

  def save(progress: Progress): IO[Unit] =
    new GameDiff(encode(progress.origin), encode(progress.game))() |> { diffs ⇒
      if (diffs.nonEmpty) {
        val fullDiffs = ("updatedAt" -> new Date()) :: diffs
        io { update(DBObject("_id" -> progress.origin.id), $set(fullDiffs: _*)) }
      }
      else io()
    }

  def insert(game: DbGame): IO[Option[String]] = io {
    insert(encode(game))
  }

  // makes the asumption that player 0 is white!
  // proved to be true on prod DB at March 31 2012
  def setEloDiffs(id: String, white: Int, black: Int) = io {
    update(
      DBObject("_id" -> id),
      $set("players.0.eloDiff" -> white, "players.1.eloDiff" -> black)
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
          "players.1.isOfferingDraw"
        )
    )
  }

  val findOneStandardCheckmate: IO[Option[DbGame]] = io {
    find(DBObject(
      "status" -> Mate.id,
      "v" -> Standard.id
    ))
      .sort(DBObject("createdAt" -> -1))
      .limit(1) 
      .toList.map(decode).flatten.headOption
  }

  def decode(raw: RawDbGame): Option[DbGame] = raw.decode

  def encode(dbGame: DbGame): RawDbGame = RawDbGame encode dbGame

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

  def candidatesToAutofinish: IO[List[DbGame]] = io {
    find(
      ("clock.l" $exists true) ++
        ("status" -> Started.id) ++
        ("updatedAt" $lt (DateTime.now - 2.hour))
    ).toList.map(decode).flatten
  }

  val countAll: IO[Int] = io { count().toInt }

  val countPlaying: IO[Int] = io {
    count("updatedAt" $gt (DateTime.now - 15.seconds)).toInt
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
