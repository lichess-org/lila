package lila.round

import chess.format.Uci
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

import Forecast.Step

final class ForecastApi(coll: Coll, roundApi: lila.core.round.RoundApi)(using Executor):

  private given BSONDocumentHandler[Step]     = Macros.handler
  private given BSONDocumentHandler[Forecast] = Macros.handler

  private def saveSteps(pov: Pov, steps: Forecast.Steps): Funit =
    lila.mon.round.forecast.create.increment()
    coll.update
      .one(
        $id(pov.fullId),
        Forecast(
          _id = pov.fullId,
          steps = steps.filter(_.nonEmpty),
          date = nowInstant
        ).truncate,
        upsert = true
      )
      .void

  def save(pov: Pov, steps: Forecast.Steps): Funit =
    firstStep(steps) match
      case None                                       => coll.delete.one($id(pov.fullId)).void
      case Some(step) if pov.game.ply == step.ply - 1 => saveSteps(pov, steps)
      case _                                          => fufail(Forecast.OutOfSync)

  def playAndSave(
      pov: Pov,
      uciMove: String,
      steps: Forecast.Steps
  ): Funit =
    if !pov.isMyTurn then funit
    else
      Uci
        .Move(uciMove)
        .fold[Funit](fufail(lila.core.round.ClientError(s"Invalid move $uciMove on $pov"))): uci =>
          val promise = Promise[Unit]()
          roundApi.tell(
            pov.gameId,
            HumanPlay(
              playerId = pov.playerId,
              uci = uci,
              blur = true,
              promise = promise.some
            )
          )
          saveSteps(pov, steps) >> promise.future

  def loadForDisplay(pov: Pov): Fu[Option[Forecast]] =
    pov.forecastable
      .so(coll.byId[Forecast](pov.fullId))
      .flatMap:
        case None => fuccess(none)
        case Some(fc) =>
          if firstStep(fc.steps).exists(_.ply != pov.game.ply + 1) then clearPov(pov).inject(none)
          else fuccess(fc.some)

  def loadForPlay(pov: Pov): Fu[Option[Forecast]] =
    pov.game.forecastable
      .so(coll.byId[Forecast](pov.fullId))
      .flatMap:
        case None => fuccess(none)
        case Some(fc) =>
          if firstStep(fc.steps).exists(_.ply != pov.game.ply) then clearPov(pov).inject(none)
          else fuccess(fc.some)

  def nextMove(g: Game, last: chess.Move): Fu[Option[Uci.Move]] =
    g.forecastable.so:
      val pov = Pov(g, g.turnColor)
      loadForPlay(pov).flatMap:
        case None => fuccess(none)
        case Some(fc) =>
          fc(g, last) match
            case Some(newFc, uciMove) if newFc.steps.nonEmpty =>
              coll.update.one($id(fc._id), newFc).inject(uciMove.some)
            case Some(_, uciMove) => clearPov(pov).inject(uciMove.some)
            case _                => clearPov(pov).inject(none)

  private def firstStep(steps: Forecast.Steps) = steps.headOption.flatMap(_.headOption)

  def clearGame(g: Game) = coll.delete.one($inIds(Color.all.map(g.fullIdOf))).void

  def clearPov(pov: Pov) = coll.delete.one($id(pov.fullId)).void
