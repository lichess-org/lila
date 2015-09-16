package lila.round

import reactivemongo.bson._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits._

import lila.game.Game
import Forecast.Step
import chess.Pos

private[round] final class ForecastApi(coll: Coll) {

  private implicit val PosBSONHandler = new BSONHandler[BSONString, Pos] {
    def read(bsonStr: BSONString): Pos = Pos.posAt(bsonStr.value) err s"No such pos: ${bsonStr.value}"
    def write(x: Pos) = BSONString(x.key)
  }

  private implicit val stepBSONHandler = Macros.handler[Step]
  private implicit val forecastBSONHandler = Macros.handler[Forecast]
  import Forecast._

  def nextMove(g: Game): Fu[Option[String]] = ???

  def clearGame(g: Game) = g.forecastable ?? coll.remove(BSONDocument(
    "_id" -> BSONDocument("$in" -> chess.Color.all.map(g.fullIdOf))
  )).void
}
