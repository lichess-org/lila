package lila.system

import model._
import DbGame._

import lila.chess.Color

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.NonEmptyList
import scalaz.effects._

class GameRepo(collection: MongoCollection)
    extends SalatDAO[RawDbGame, String](collection) {

  def game(gameId: String): IO[DbGame] = io {
    if (gameId.size != gameIdSize)
      throw new Exception("Invalid game id " + gameId)
    findOneByID(gameId) flatMap decode err "No game found for id " + gameId
  }

  def player(gameId: String, color: Color): IO[(DbGame, DbPlayer)] =
    game(gameId) map { g ⇒ (g, g player color) }

  def playerOnly(gameId: String, color: Color): IO[DbPlayer] =
    game(gameId) map { g ⇒ g player color }

  def player(fullId: String): IO[(DbGame, DbPlayer)] =
    game(fullId take gameIdSize) map { g ⇒
      val playerId = fullId drop gameIdSize
      val player = g player playerId err "No player found for id " + fullId
      (g, player)
    }

  def playerGame(fullId: String): IO[DbGame] =
    player(fullId) map (_._1)

  def save(game: DbGame): IO[Unit] = io {
    update(DBObject("_id" -> game.id), _grater asDBObject encode(game), false, false)
  }

  def applyDiff(a: DbGame, b: DbGame): IO[Unit] = io {
    update(DBObject("_id" -> a.id), diff(encode(a), encode(b)), false, false)
  }

  private def diff(a: RawDbGame, b: RawDbGame): DBObject = {
    val builder = MongoDBObject.newBuilder
    def d[A](name: String, f: RawDbGame ⇒ A) {
      if (f(a) != f(b)) builder += name -> f(b)
    }
    d("pgn", _.pgn)
    d("status", _.status)
    d("turns", _.turns)
    d("lastMove", _.lastMove)
    d("positionHashes", _.positionHashes)
    d("castles", _.castles)
    for (i ← 0 to 1) {
      val name = "players." + i + "."
      d(name + "ps", _.players(i).ps)
      d(name + "isWinner", _.players(i).isWinner)
      d(name + "evts", _.players(i).evts)
    }
    a.clock foreach { c ⇒
      d("clock.color", _.clock.get.color)
      d("clock.times", _.clock.get.times)
      d("clock.timer", _.clock.get.timer)
    }

    MongoDBObject("$set" -> builder.result)
  }

  def insert(game: DbGame): IO[Option[String]] = io {
    insert(encode(game))
  }

  def decode(raw: RawDbGame): Option[DbGame] = raw.decode

  def encode(dbGame: DbGame): RawDbGame = RawDbGame encode dbGame
}
