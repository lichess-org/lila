package lila.round

import reactivemongo.api.bson._

import lila.db.dsl._
import org.joda.time.DateTime
import scala.concurrent.Promise

import shogi.format.usi.{ UciToUsi, Usi }
import Forecast.Step
import lila.game.Game.PlayerId
import lila.game.{ Game, Pov }

final class ForecastApi(coll: Coll, tellRound: TellRound)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val stepBSONHandler     = Macros.handler[Step]
  implicit private val forecastBSONHandler = Macros.handler[Forecast]

  private def saveSteps(pov: Pov, steps: Forecast.Steps): Funit = {
    lila.mon.round.forecast.create.increment()
    coll.update
      .one(
        $id(pov.fullId),
        Forecast(
          _id = pov.fullId,
          steps = steps,
          date = DateTime.now
        ).truncate,
        upsert = true
      )
      .void
  }

  def save(pov: Pov, steps: Forecast.Steps): Funit =
    firstStep(steps) match {
      case None                                         => coll.delete.one($id(pov.fullId)).void
      case Some(step) if pov.game.plies == step.ply - 1 => saveSteps(pov, steps)
      case _                                            => fufail(Forecast.OutOfSync)
    }

  def playAndSave(
      pov: Pov,
      usiMove: String,
      steps: Forecast.Steps
  ): Funit =
    if (!pov.isMyTurn) funit
    else
      Usi(usiMove).orElse(UciToUsi(usiMove)).fold[Funit](fufail(s"Invalid move $usiMove on $pov")) { usi =>
        val promise = Promise[Unit]()
        tellRound(
          pov.gameId,
          actorApi.round.HumanPlay(
            playerId = PlayerId(pov.playerId),
            usi = usi,
            blur = true,
            promise = promise.some
          )
        )
        saveSteps(pov, steps) >> promise.future
      }

  def loadForDisplay(pov: Pov): Fu[Option[Forecast]] =
    pov.forecastable ?? coll.ext.find($id(pov.fullId)).one[Forecast] flatMap {
      case None => fuccess(none)
      case Some(fc) =>
        if (firstStep(fc.steps).exists(_.ply != pov.game.plies + 1)) clearPov(pov) inject none
        else fuccess(fc.some)
    }

  def loadForPlay(pov: Pov): Fu[Option[Forecast]] =
    pov.game.forecastable ?? coll.ext.find($id(pov.fullId)).one[Forecast] flatMap {
      case None => fuccess(none)
      case Some(fc) =>
        if (firstStep(fc.steps).exists(_.ply != pov.game.plies)) clearPov(pov) inject none
        else fuccess(fc.some)
    }

  def nextMove(g: Game, last: Usi): Fu[Option[Usi]] =
    g.forecastable ?? {
      loadForPlay(Pov player g) flatMap {
        case None => fuccess(none)
        case Some(fc) =>
          fc(g, last) match {
            case Some((newFc, usi)) if newFc.steps.nonEmpty =>
              coll.update.one($id(fc._id), newFc) inject usi.some
            case Some((_, usi)) => clearPov(Pov player g) inject usi.some
            case _              => clearPov(Pov player g) inject none
          }
      }
    }

  private def firstStep(steps: Forecast.Steps) = steps.headOption.flatMap(_.headOption)

  def clearGame(g: Game) = coll.delete.one($inIds(shogi.Color.all.map(g.fullIdOf))).void

  def clearPov(pov: Pov) = coll.delete.one($id(pov.fullId)).void
}
