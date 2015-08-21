package lila.round

import reactivemongo.bson._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits._

import lila.game.Game

private[round] final class PremoveApi(coll: Coll) {

  private implicit val premoveBSONHandler = Macros.handler[Premove]
  import Premove._

  def nextMove(g: Game): Fu[Option[String]] = ???

  def clearGame(g: Game) = gameQualifies(g) ?? coll.remove(BSONDocument(
    "_id" -> BSONDocument("$in" -> chess.Color.all.map(g.fullIdOf))
  )).void
}
