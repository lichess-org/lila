package lila.system
package db

import model._
import DbGame._

import lila.chess.Color
import lila.chess.format.Forsyth

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

class GameRepo(collection: MongoCollection)
    extends SalatDAO[RawDbGame, String](collection) {

  def game(gameId: String): IO[DbGame] = io {
    if (gameId.size != gameIdSize)
      throw new Exception("Invalid game id " + gameId)
    findOneByID(gameId) flatMap decode err "No game found for id " + gameId
  }

  def pov(gameId: String, color: Color): IO[Pov] =
    game(gameId) map { g ⇒ Pov(g, g player color) }

  def player(gameId: String, color: Color): IO[DbPlayer] =
    game(gameId) map { g ⇒ g player color }

  def pov(fullId: String): IO[Pov] =
    game(fullId take gameIdSize) map { g ⇒
      val playerId = fullId drop gameIdSize
      val player = g player playerId err "No player found for id " + fullId
      Pov(g, player)
    }

  def save(game: DbGame): IO[Unit] = io {
    update(DBObject("_id" -> game.id), _grater asDBObject encode(game))
  }

  def applyDiff(a: DbGame, b: DbGame): IO[Unit] = io {
    update(DBObject("_id" -> a.id), diff(encode(a), encode(b)))
  }

  def diff(a: RawDbGame, b: RawDbGame): MongoDBObject = {
    val builder = MongoDBObject.newBuilder
    def d[A](name: String, f: RawDbGame ⇒ A) {
      if (f(a) != f(b)) builder += name -> f(b)
    }
    d("pgn", _.pgn)
    d("status", _.status)
    d("turns", _.turns)
    d("lastMove", _.lastMove)
    d("check", _.check)
    d("positionHashes", _.positionHashes)
    d("castles", _.castles)
    for (i ← 0 to 1) {
      val name = "players." + i + "."
      d(name + "ps", _.players(i).ps)
      d(name + "w", _.players(i).w)
      d(name + "evts", _.players(i).evts)
      d(name + "lastDrawOffer", _.players(i).lastDrawOffer)
      d(name + "isOfferingDraw", _.players(i).isOfferingDraw)
    }
    a.clock foreach { c ⇒
      d("clock.c", _.clock.get.c)
      d("clock.w", _.clock.get.w)
      d("clock.b", _.clock.get.b)
      d("clock.timer", _.clock.get.timer)
    }

    MongoDBObject("$set" -> builder.result)
  }

  def insert(game: DbGame): IO[Option[String]] = io {
    insert(encode(game))
  }

  def decode(raw: RawDbGame): Option[DbGame] = raw.decode

  def encode(dbGame: DbGame): RawDbGame = RawDbGame encode dbGame

  def saveInitialFen(dbGame: DbGame): IO[Unit] = io {
    update(
      DBObject("_id" -> dbGame.id),
      $set ("initialFen" -> (Forsyth >> dbGame.toChess))
    )
  }

  def ensureIndexes: IO[Unit] = io {
    collection.ensureIndex(DBObject("status" -> 1))
    collection.ensureIndex(DBObject("userIds" -> 1))
    collection.ensureIndex(DBObject("winnerUserId" -> 1))
    collection.ensureIndex(DBObject("turns" -> 1))
    collection.ensureIndex(DBObject("updatedAt" -> -1))
    collection.ensureIndex(DBObject("createdAt" -> -1))
    collection.ensureIndex(DBObject("createdAt" -> -1, "userIds" -> 1))
  }

  def dropIndexes: IO[Unit] = io {
    collection.dropIndexes()
  }
}
