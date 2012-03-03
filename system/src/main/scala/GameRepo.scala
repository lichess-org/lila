package lila.system

import model._
import DbGame._

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

class GameRepo(collection: MongoCollection) extends SalatDAO[DbGame, String](collection) {

  def game(gameId: String): Option[DbGame] =
    if (gameId.size == gameIdSize) findOneByID(gameId)
    else None

  def player(fullId: String): Option[(DbGame, DbPlayer)] = for {
    game ← game(fullId take gameIdSize)
    player ← game playerById (fullId drop gameIdSize)
  } yield (game, player)

  def player(gameId: String, color: String): Option[(DbGame, DbPlayer)] = for {
    game ← game(gameId take gameIdSize)
    player ← game playerByColor color
  } yield (game, player)

  override def save(game: DbGame) {
    update(DBObject("_id" -> game.id), _grater asDBObject game, false, false)
  }

  def anyGame = findOne(DBObject())
}
