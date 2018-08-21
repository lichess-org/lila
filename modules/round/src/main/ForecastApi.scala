package lila.round

import reactivemongo.bson._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import org.joda.time.DateTime
import scala.concurrent.Promise

import chess.format.Uci
import chess.Pos
import Forecast.Step
import lila.game.{ Pov, Game }
import lila.hub.DuctMap

final class ForecastApi(coll: Coll, roundMap: DuctMap[Round]) {

  private implicit val PosBSONHandler = new BSONHandler[BSONString, Pos] {
    def read(bsonStr: BSONString): Pos = Pos.posAt(bsonStr.value) err s"No such pos: ${bsonStr.value}"
    def write(x: Pos) = BSONString(x.key)
  }

  private implicit val stepBSONHandler = Macros.handler[Step]
  private implicit val forecastBSONHandler = Macros.handler[Forecast]

  private def saveSteps(pov: Pov, steps: Forecast.Steps): Funit = {
    lila.mon.round.forecast.create()
    coll.update(
      $id(pov.fullId),
      Forecast(
        _id = pov.fullId,
        steps = steps,
        date = DateTime.now
      ).truncate,
      upsert = true
    ).void
  }

  def save(pov: Pov, steps: Forecast.Steps): Funit = firstStep(steps) match {
    case None => coll.remove($id(pov.fullId)).void
    case Some(step) if pov.game.turns == step.ply - 1 => saveSteps(pov, steps)
    case _ => fufail(Forecast.OutOfSync)
  }

  def playAndSave(
    pov: Pov,
    uciMove: String,
    steps: Forecast.Steps
  ): Funit =
    if (!pov.isMyTurn) funit
    else Uci.Move(uciMove).fold[Funit](fufail(s"Invalid move $uciMove on $pov")) { uci =>
      val promise = Promise[Unit]
      roundMap.tell(pov.gameId, actorApi.round.HumanPlay(
        playerId = pov.playerId,
        uci = uci,
        blur = true,
        promise = promise.some
      ))
      saveSteps(pov, steps) >> promise.future
    }

  def loadForDisplay(pov: Pov): Fu[Option[Forecast]] =
    pov.forecastable ?? coll.find($id(pov.fullId)).uno[Forecast] flatMap {
      case None => fuccess(none)
      case Some(fc) =>
        if (firstStep(fc.steps).exists(_.ply != pov.game.turns + 1)) clearPov(pov) inject none
        else fuccess(fc.some)
    }

  def loadForPlay(pov: Pov): Fu[Option[Forecast]] =
    pov.game.forecastable ?? coll.find($id(pov.fullId)).uno[Forecast] flatMap {
      case None => fuccess(none)
      case Some(fc) =>
        if (firstStep(fc.steps).exists(_.ply != pov.game.turns)) clearPov(pov) inject none
        else fuccess(fc.some)
    }

  def nextMove(g: Game, last: chess.Move): Fu[Option[Uci.Move]] = g.forecastable ?? {
    loadForPlay(Pov player g) flatMap {
      case None => fuccess(none)
      case Some(fc) => fc(g, last) match {
        case Some((newFc, uciMove)) if newFc.steps.nonEmpty =>
          coll.update($id(fc._id), newFc) inject uciMove.some
        case Some((newFc, uciMove)) => clearPov(Pov player g) inject uciMove.some
        case _ => clearPov(Pov player g) inject none
      }
    }
  }

  private def firstStep(steps: Forecast.Steps) = steps.headOption.flatMap(_.headOption)

  def clearGame(g: Game) = coll.remove($inIds(chess.Color.all.map(g.fullIdOf))).void

  def clearPov(pov: Pov) = coll.remove($id(pov.fullId)).void
}
