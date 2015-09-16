package lila.round

import reactivemongo.bson._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits._
import org.joda.time.DateTime

import chess.Pos
import Forecast.Step
import lila.game.{ Pov, Game }

final class ForecastApi(coll: Coll) {

  private implicit val PosBSONHandler = new BSONHandler[BSONString, Pos] {
    def read(bsonStr: BSONString): Pos = Pos.posAt(bsonStr.value) err s"No such pos: ${bsonStr.value}"
    def write(x: Pos) = BSONString(x.key)
  }

  private implicit val stepBSONHandler = Macros.handler[Step]
  private implicit val forecastBSONHandler = Macros.handler[Forecast]
  import Forecast._

  def save(pov: Pov, steps: Forecast.Steps): Funit =
    steps.headOption.flatMap(_.headOption) match {
      case None => coll.remove(BSONDocument("_id" -> pov.fullId)).void
      case Some(step) if pov.game.turns == step.ply - 1 => coll.update(
        BSONDocument("_id" -> pov.fullId),
        Forecast(
          _id = pov.fullId,
          ply = step.ply,
          steps = steps,
          date = DateTime.now),
        upsert = true).void
      case _ => funit // UI is out of sync with the game
    }

  def load(pov: Pov): Fu[Option[Forecast]] =
    pov.forecastable ?? coll.find(BSONDocument("_id" -> pov.fullId)).one[Forecast]

  def nextMove(g: Game): Fu[Option[String]] = ???

  def clearGame(g: Game) = g.forecastable ?? coll.remove(BSONDocument(
    "_id" -> BSONDocument("$in" -> chess.Color.all.map(g.fullIdOf))
  )).void
}
