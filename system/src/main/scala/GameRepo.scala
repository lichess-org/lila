package lila.system

import model._
import DbGame._

import lila.chess.Color

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

class GameRepo(collection: MongoCollection)
extends SalatDAO[RawDbGame, String](collection) {

  def game(gameId: String): Option[DbGame] =
    if (gameId.size == gameIdSize) findOneByID(gameId) flatMap decode
    else None

  def player(fullId: String): Option[(DbGame, DbPlayer)] = for {
    game ← game(fullId take gameIdSize)
    player ← game playerById (fullId drop gameIdSize)
  } yield (game, player)

  def player(gameId: String, color: Color): Option[(DbGame, DbPlayer)] = for {
    game ← game(gameId take gameIdSize)
    player ← game playerByColor color
  } yield (game, player)

  def save(game: DbGame) =
    update(DBObject("_id" -> game.id), _grater asDBObject encode(game), false, false)

  def insert(game: DbGame): Option[String] = insert(encode(game))

  def anyGame = findOne(DBObject()) flatMap decode

  private def decode(raw: RawDbGame): Option[DbGame] = raw.decode

  private def encode(dbGame: DbGame): RawDbGame = RawDbGame encode dbGame
}
