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

  def game(gameId: String): IO[Valid[DbGame]] = io {
    if (gameId.size == gameIdSize)
      findOneByID(gameId) flatMap decode toValid "No game found for id " + gameId
    else failure(NonEmptyList("Invalid game id " + gameId))
  }

  def player(gameId: String, color: Color): IO[Valid[(DbGame, DbPlayer)]] = for {
    validGame ← game(gameId)
  } yield for {
    game ← validGame
  } yield (game, game player color)

  def player(fullId: String): IO[Valid[(DbGame, DbPlayer)]] = for {
    validGame ← game(fullId take gameIdSize)
  } yield for {
    game ← validGame
    playerId = fullId drop gameIdSize
    player ← game player playerId toSuccess NonEmptyList("No player found for id " + playerId)
  } yield (game, player)

  def playerGame(fullId: String): IO[Valid[DbGame]] = for {
    someGameAndPlayer ← player(fullId)
  } yield for {
    gameAndPlayer ← someGameAndPlayer
    (game, player) = gameAndPlayer
  } yield game

  def save(game: DbGame): IO[Unit] = io {
    update(DBObject("_id" -> game.id), _grater asDBObject encode(game), false, false)
  }

  def insert(game: DbGame): IO[Option[String]] = io {
    insert(encode(game))
  }

  def decode(raw: RawDbGame): Option[DbGame] = raw.decode

  def encode(dbGame: DbGame): RawDbGame = RawDbGame encode dbGame
}
