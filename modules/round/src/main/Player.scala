package lila.round

import actorApi.round.{ DrawNo, ForecastPlay, HumanPlay, TakebackNo, TooManyPlies }
import chess.format.{ Fen, Uci }
import chess.{ Centis, Clock, MoveMetrics, MoveOrDrop, Status, ErrorStr }
import chess.MoveOrDrop.*
import java.util.concurrent.TimeUnit

import lila.common.Bus
import lila.game.actorApi.MoveGameEvent
import lila.game.{ Game, Pov, Progress, UciMemo }

final private class Player(
    fishnetPlayer: lila.fishnet.FishnetPlayer,
    finisher: Finisher,
    scheduleExpiration: ScheduleExpiration,
    uciMemo: UciMemo
)(using Executor):

  sealed private trait MoveResult
  private case object Flagged extends MoveResult
  private case class MoveApplied(progress: Progress, move: MoveOrDrop, compedLag: Option[Centis])
      extends MoveResult

  private[round] def human(play: HumanPlay, round: RoundAsyncActor)(
      pov: Pov
  )(using proxy: GameProxy): Fu[Events] =
    play match
      case HumanPlay(_, uci, blur, lag, _) =>
        pov match
          case Pov(game, _) if game.ply > Game.maxPlies =>
            round ! TooManyPlies
            fuccess(Nil)
          case Pov(game, color) if game playableBy color =>
            applyUci(game, uci, blur, lag)
              .leftMap(e => s"$pov $e")
              .fold(errs => fufail(ClientError(errs)), fuccess)
              .flatMap {
                case Flagged => finisher.outOfTime(game)
                case MoveApplied(progress, moveOrDrop, compedLag) =>
                  compedLag foreach { lag =>
                    lila.mon.round.move.lag.moveComp.record(lag.millis, TimeUnit.MILLISECONDS)
                  }
                  proxy.save(progress) >>
                    postHumanOrBotPlay(round, pov, progress, moveOrDrop)
              }
          case Pov(game, _) if game.finished           => fufail(GameIsFinishedError(pov))
          case Pov(game, _) if game.aborted            => fufail(ClientError(s"$pov game is aborted"))
          case Pov(game, color) if !game.turnOf(color) => fufail(ClientError(s"$pov not your turn"))
          case _ => fufail(ClientError(s"$pov move refused for some reason"))

  private[round] def bot(uci: Uci, round: RoundAsyncActor)(pov: Pov)(using proxy: GameProxy): Fu[Events] =
    pov match
      case Pov(game, _) if game.ply > Game.maxPlies =>
        round ! TooManyPlies
        fuccess(Nil)
      case Pov(game, color) if game playableBy color =>
        applyUci(game, uci, blur = false, botLag)
          .fold(errs => fufail(ClientError(ErrorStr raw errs)), fuccess)
          .flatMap {
            case Flagged => finisher.outOfTime(game)
            case MoveApplied(progress, moveOrDrop, _) =>
              proxy.save(progress) >> postHumanOrBotPlay(round, pov, progress, moveOrDrop)
          }
      case Pov(game, _) if game.finished           => fufail(GameIsFinishedError(pov))
      case Pov(game, _) if game.aborted            => fufail(ClientError(s"$pov game is aborted"))
      case Pov(game, color) if !game.turnOf(color) => fufail(ClientError(s"$pov not your turn"))
      case _ => fufail(ClientError(s"$pov move refused for some reason"))

  private def postHumanOrBotPlay(
      round: RoundAsyncActor,
      pov: Pov,
      progress: Progress,
      moveOrDrop: MoveOrDrop
  )(using GameProxy): Fu[Events] =
    if pov.game.hasAi then uciMemo.add(pov.game, moveOrDrop)
    notifyMove(moveOrDrop, progress.game)
    if progress.game.finished then moveFinish(progress.game) dmap { progress.events ::: _ }
    else
      if progress.game.playableByAi then requestFishnet(progress.game, round)
      if pov.opponent.isOfferingDraw then round ! DrawNo(pov.player.id)
      if pov.player.isProposingTakeback then round ! TakebackNo(pov.player.id)
      if progress.game.forecastable then
        moveOrDrop.move.foreach { move =>
          round ! ForecastPlay(move)
        }
      scheduleExpiration(progress.game)
      fuccess(progress.events)

  private[round] def fishnet(game: Game, sign: String, uci: Uci)(using proxy: GameProxy): Fu[Events] =
    if game.playable && game.player.isAi then
      uciMemo sign game flatMap { expectedSign =>
        if expectedSign == sign then
          applyUci(game, uci, blur = false, metrics = fishnetLag)
            .fold(errs => fufail(ClientError(ErrorStr raw errs)), fuccess)
            .flatMap:
              case Flagged => finisher.outOfTime(game)
              case MoveApplied(progress, moveOrDrop, _) =>
                proxy
                  .save(progress)
                  .andDo:
                    uciMemo.add(progress.game, moveOrDrop)
                    lila.mon.fishnet.move(~game.aiLevel).increment()
                    notifyMove(moveOrDrop, progress.game)
                  .>> {
                    if progress.game.finished then moveFinish(progress.game) dmap { progress.events ::: _ }
                    else fuccess(progress.events)
                  }
        else
          fufail:
            FishnetError:
              s"Invalid game hash: $sign id: ${game.id} playable: ${game.playable} player: ${game.player}"
      }
    else
      fufail:
        FishnetError:
          s"Not AI turn move: $uci id: ${game.id} playable: ${game.playable} player: ${game.player}"

  private[round] def requestFishnet(game: Game, round: RoundAsyncActor): Funit =
    game.playableByAi.so:
      if game.ply <= fishnetPlayer.maxPlies then fishnetPlayer(game)
      else fuccess(round ! actorApi.round.ResignAi)

  private val fishnetLag = MoveMetrics(clientLag = Centis(5).some)
  private val botLag     = MoveMetrics(clientLag = Centis(0).some)

  private def applyUci(
      game: Game,
      uci: Uci,
      blur: Boolean,
      metrics: MoveMetrics
  ): Either[ErrorStr, MoveResult] =
    (uci match
      case Uci.Move(orig, dest, prom) =>
        game.chess.moveWithCompensated(orig, dest, prom, metrics) map { (ncg, move) =>
          ncg -> move
        }
      case Uci.Drop(role, pos) =>
        game.chess.drop(role, pos, metrics) map { (ncg, drop) =>
          Clock.WithCompensatedLag(ncg, None) -> drop
        }
    ).map {
      case (ncg, _) if ncg.value.clock.exists(_.outOfTime(game.turnColor, withGrace = false)) => Flagged
      case (ncg, moveOrDrop: MoveOrDrop) =>
        MoveApplied(
          game.update(ncg.value, moveOrDrop, blur),
          moveOrDrop,
          ncg.compensated
        )
    }

  private def notifyMove(moveOrDrop: MoveOrDrop, game: Game): Unit =
    import lila.hub.actorApi.round.{ CorresMoveEvent, MoveEvent, SimulMoveEvent }
    val color = moveOrDrop.color
    val moveEvent = MoveEvent(
      gameId = game.id,
      fen = Fen write game.chess,
      move = moveOrDrop.fold(_.toUci.keys, _.toUci.uci)
    )

    // I checked and the bus doesn't do much if there's no subscriber for a classifier,
    // so we should be good here.
    // also used for targeted TvBroadcast subscription
    Bus.publish(MoveGameEvent(game, moveEvent.fen, moveEvent.move), MoveGameEvent makeChan game.id)

    // publish correspondence moves
    if game.isCorrespondence && game.nonAi then
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
    for
      simulId        <- game.simulId
      opponentUserId <- game.player(!color).userId
    yield Bus.publish(
      SimulMoveEvent(move = moveEvent, simulId = simulId, opponentUserId = opponentUserId),
      "moveEventSimul"
    )

  private def moveFinish(game: Game)(using GameProxy): Fu[Events] =
    game.status match
      case Status.Mate       => finisher.other(game, _.Mate, game.situation.winner)
      case Status.VariantEnd => finisher.other(game, _.VariantEnd, game.situation.winner)
      case status @ (Status.Stalemate | Status.Draw) => finisher.other(game, _ => status, None)
      case _                                         => fuccess(Nil)
