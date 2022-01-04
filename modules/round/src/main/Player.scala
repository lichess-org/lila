package lila.round

import shogi.format.Forsyth
import shogi.format.usi.Usi
import shogi.{ Centis, MoveMetrics, MoveOrDrop, Status }

import actorApi.round.{ DrawNo, ForecastPlay, HumanPlay, TakebackNo, TooManyPlies }
import lila.game.actorApi.MoveGameEvent
import lila.common.Bus
import lila.game.{ Event, Game, Pov, Progress }
import lila.game.Game.PlayerId
import cats.data.Validated

final private class Player(
    fishnetPlayer: lila.fishnet.Player,
    finisher: Finisher,
    scheduleExpiration: ScheduleExpiration
)(implicit ec: scala.concurrent.ExecutionContext) {

  sealed private trait MoveResult
  private case object Flagged                                          extends MoveResult
  private case class MoveApplied(progress: Progress, move: MoveOrDrop) extends MoveResult

  private[round] def human(play: HumanPlay, round: RoundDuct)(
      pov: Pov
  )(implicit proxy: GameProxy): Fu[Events] =
    play match {
      case HumanPlay(_, usi, blur, lag, _) =>
        pov match {
          case Pov(game, _) if game.turns > Game.maxPlies =>
            round ! TooManyPlies
            fuccess(Nil)
          case Pov(game, color) if game playableBy color =>
            applyUsi(game, usi, blur, lag)
              .leftMap(e => s"$pov $e")
              .fold(errs => fufail(ClientError(errs.toString)), fuccess)
              .flatMap {
                case Flagged => finisher.outOfTime(game)
                case MoveApplied(progress, moveOrDrop) =>
                  proxy.save(progress) >>
                    postHumanOrBotPlay(round, pov, progress, moveOrDrop)
              }
          case Pov(game, _) if game.finished           => fufail(ClientError(s"$pov game is finished"))
          case Pov(game, _) if game.aborted            => fufail(ClientError(s"$pov game is aborted"))
          case Pov(game, color) if !game.turnOf(color) => fufail(ClientError(s"$pov not your turn"))
          case _                                       => fufail(ClientError(s"$pov move refused for some reason"))
        }
    }

  private[round] def bot(usi: Usi, round: RoundDuct)(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    pov match {
      case Pov(game, _) if game.turns > Game.maxPlies =>
        round ! TooManyPlies
        fuccess(Nil)
      case Pov(game, color) if game playableBy color =>
        applyUsi(game, usi, false, botLag)
          .fold(errs => fufail(ClientError(errs.toString)), fuccess)
          .flatMap {
            case Flagged => finisher.outOfTime(game)
            case MoveApplied(progress, moveOrDrop) =>
              proxy.save(progress) >> postHumanOrBotPlay(round, pov, progress, moveOrDrop)
          }
      case Pov(game, _) if game.finished           => fufail(ClientError(s"$pov game is finished"))
      case Pov(game, _) if game.aborted            => fufail(ClientError(s"$pov game is aborted"))
      case Pov(game, color) if !game.turnOf(color) => fufail(ClientError(s"$pov not your turn"))
      case _                                       => fufail(ClientError(s"$pov move refused for some reason"))
    }

  private def postHumanOrBotPlay(
      round: RoundDuct,
      pov: Pov,
      progress: Progress,
      moveOrDrop: MoveOrDrop
  )(implicit proxy: GameProxy): Fu[Events] = {
    notifyMove(moveOrDrop, progress.game)
    if (progress.game.finished) moveFinish(progress.game) dmap { progress.events ::: _ }
    else {
      if (progress.game.playableByAi) requestFishnet(progress.game, round)
      if (pov.opponent.isOfferingDraw) round ! DrawNo(PlayerId(pov.player.id))
      if (pov.player.isProposingTakeback) round ! TakebackNo(PlayerId(pov.player.id))
      if (progress.game.forecastable)
        moveOrDrop.fold(
          move => round ! ForecastPlay(move.toUsi),
          drop => round ! ForecastPlay(drop.toUsi)
        )
      scheduleExpiration(progress.game)
      fuccess(progress.events)
    }
  }

  private[round] def fishnet(game: Game, ply: Int, usi: Usi)(implicit proxy: GameProxy): Fu[Events] = {
    if (game.playable && game.player.isAi && game.playedTurns == ply) {
      applyUsi(game, usi, blur = false, metrics = fishnetLag)
        .fold(errs => fufail(ClientError(errs.toString)), fuccess)
        .flatMap {
          case Flagged => finisher.outOfTime(game)
          case MoveApplied(progress, moveOrDrop) =>
            proxy.save(progress) >>-
              notifyMove(moveOrDrop, progress.game) >> {
                if (progress.game.finished) moveFinish(progress.game) dmap { progress.events ::: _ }
                else
                  fuccess(progress.events :+ Event.Reload)
              }
        }
    } else
      fufail(
        FishnetError(
          s"Not AI turn move: ${usi} id: ${game.id} playable: ${game.playable} player: ${game.player}"
        )
      )
  }

  private[round] def requestFishnet(game: Game, round: RoundDuct): Funit =
    game.playableByAi ?? {
      if (game.turns <= fishnetPlayer.maxPlies) fishnetPlayer(game)
      else fuccess(round ! actorApi.round.ResignAi)
    }

  private val fishnetLag = MoveMetrics(clientLag = Centis(5).some)
  private val botLag     = MoveMetrics(clientLag = Centis(10).some)

  private def applyUsi(
      game: Game,
      usi: Usi,
      blur: Boolean,
      metrics: MoveMetrics
  ): Validated[String, MoveResult] = {
    (usi match {
      case Usi.Move(orig, dest, prom) => {
        game.shogi(orig, dest, prom, metrics) map {
          case (nsg, move) => {
            nsg -> (Left(move): MoveOrDrop)
          }
        }
      }
      case Usi.Drop(role, pos) =>
        game.shogi.drop(role, pos, metrics) map { case (nsg, drop) =>
          nsg -> (Right(drop): MoveOrDrop)
        }
    }).map {
      case (nsg, _) if nsg.clock.exists(_.outOfTime(game.turnColor, withGrace = false)) =>
        Flagged
      case (newShogiGame, moveOrDrop) =>
        MoveApplied(
          game.update(newShogiGame, moveOrDrop, blur),
          moveOrDrop
        )
    }
  }

  private def notifyMove(moveOrDrop: MoveOrDrop, game: Game): Unit = {
    import lila.hub.actorApi.round.{ CorresMoveEvent, MoveEvent, SimulMoveEvent }
    val color = moveOrDrop.fold(_.color, _.color)
    val moveEvent = MoveEvent(
      gameId = game.id,
      fen = Forsyth exportSituation game.situation,
      move = moveOrDrop.fold(_.toUsi.usiKeys, _.toUsi.usi)
    )

    // I checked and the bus doesn't do much if there's no subscriber for a classifier,
    // so we should be good here.
    // also used for targeted TvBroadcast subscription
    Bus.publish(MoveGameEvent(game, moveEvent.fen, moveEvent.move), MoveGameEvent makeChan game.id)

    // publish correspondence moves
    if (game.isCorrespondence && game.nonAi)
      Bus.publish(
        CorresMoveEvent(
          move = moveEvent,
          playerUserId = game.player(color).userId,
          mobilePushable = game.mobilePushable,
          alarmable = game.alarmable,
          unlimited = game.isUnlimited
        ),
        "moveEventCorres"
      )

    // publish simul moves
    for {
      simulId        <- game.simulId
      opponentUserId <- game.player(!color).userId
    } Bus.publish(
      SimulMoveEvent(move = moveEvent, simulId = simulId, opponentUserId = opponentUserId),
      "moveEventSimul"
    )
  }

  private def moveFinish(game: Game)(implicit proxy: GameProxy): Fu[Events] = {
    game.status match {
      case Status.Mate           => finisher.other(game, _.Mate, game.situation.winner)
      case Status.Stalemate      => finisher.other(game, _.Stalemate, game.situation.winner)
      case Status.Impasse27      => finisher.other(game, _.Impasse27, game.situation.winner)
      case Status.PerpetualCheck => finisher.other(game, _.PerpetualCheck, game.situation.winner)
      case Status.VariantEnd     => finisher.other(game, _.VariantEnd, game.situation.winner)
      case Status.Draw           => finisher.other(game, _.Draw, None)
      case _                     => fuccess(Nil)
    }
  }
}
