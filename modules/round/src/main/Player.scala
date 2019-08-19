package lidraughts.round

import scala.concurrent.duration._
import scala.concurrent.Promise

import draughts.format.{ FEN, Forsyth, Uci }
import draughts.{ Centis, Color, Move, MoveMetrics, Status }

import actorApi.round.{ DrawNo, ForecastPlay, HumanPlay, TakebackNo, TooManyPlies }
import akka.actor._
import lidraughts.common.Future
import lidraughts.game.actorApi.MoveGameEvent
import lidraughts.game.{ Game, GameDiff, Pov, Progress, UciMemo }
import lidraughts.hub.actorApi.round.{ BotPlay, DraughtsnetPlay }
import ornicar.scalalib.Random.approximatly

private[round] final class Player(
    system: ActorSystem,
    draughtsnetPlayer: lidraughts.draughtsnet.Player,
    bus: lidraughts.common.Bus,
    finisher: Finisher,
    scheduleExpiration: Game => Unit,
    uciMemo: UciMemo
) {

  private sealed trait MoveResult
  private case object Flagged extends MoveResult
  private case class MoveApplied(progress: Progress, move: Move) extends MoveResult

  private[round] def human(play: HumanPlay, round: RoundDuct)(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = play match {
    case p @ HumanPlay(playerId, uci, blur, lag, promiseOption, finalSquare) => pov match {
      case Pov(game, _) if game.turns > Game.maxPlies =>
        round ! TooManyPlies
        fuccess(Nil)
      case Pov(game, color) if game playableBy color =>
        p.trace.segmentSync("applyUci", "logic")(applyUci(game, uci, blur, lag, finalSquare)).prefixFailuresWith(s"$pov ")
          .fold(errs => fufail(ClientError(errs.shows)), fuccess).flatMap {
            case Flagged => finisher.outOfTime(game)
            case MoveApplied(progress, moveOrDrop) =>
              p.trace.segment("save", "db")(proxy.save(progress)) >>
                postHumanOrBotPlay(round, pov, progress, moveOrDrop, promiseOption)
          } addFailureEffect { e =>
            promiseOption.foreach(_ failure e)
          }
      case Pov(game, _) if game.finished => fufail(ClientError(s"$pov game is finished"))
      case Pov(game, _) if game.aborted => fufail(ClientError(s"$pov game is aborted"))
      case Pov(game, color) if !game.turnOf(color) => fufail(ClientError(s"$pov not your turn"))
      case _ => fufail(ClientError(s"$pov move refused for some reason"))
    }
  }

  private[round] def bot(play: BotPlay, round: RoundDuct)(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = play match {
    case p @ BotPlay(playerId, uci, promiseOption) => pov match {
      case Pov(game, _) if game.turns > Game.maxPlies =>
        round ! TooManyPlies
        fuccess(Nil)
      case Pov(game, color) if game playableBy color =>
        applyUci(game, uci, false, botLag).prefixFailuresWith(s"$pov ")
          .fold(errs => fufail(ClientError(errs.shows)), fuccess).flatMap {
            case Flagged => finisher.outOfTime(game)
            case MoveApplied(progress, moveOrDrop) =>
              proxy.save(progress) >> postHumanOrBotPlay(round, pov, progress, moveOrDrop, promiseOption)
          } addFailureEffect { e =>
            promiseOption.foreach(_ failure e)
          }
      case Pov(game, _) if game.finished => fufail(ClientError(s"$pov game is finished"))
      case Pov(game, _) if game.aborted => fufail(ClientError(s"$pov game is aborted"))
      case Pov(game, color) if !game.turnOf(color) => fufail(ClientError(s"$pov not your turn"))
      case _ => fufail(ClientError(s"$pov move refused for some reason"))
    }
  }

  private def postHumanOrBotPlay(
    round: RoundDuct,
    pov: Pov,
    progress: Progress,
    move: Move,
    promiseOption: Option[Promise[Unit]]
  )(implicit proxy: GameProxy): Fu[Events] = {
    if (pov.game.hasAi) uciMemo.add(pov.game, move)
    notifyMove(move, progress.game)
    val res = if (progress.game.finished) moveFinish(progress.game, pov.color) dmap { progress.events ::: _ }
    else {
      if (progress.game.playableByAi) requestDraughtsnet(progress.game, round)
      if (pov.opponent.isOfferingDraw) round ! DrawNo(pov.player.id)
      if (pov.player.isProposingTakeback) round ! TakebackNo(pov.player.id)
      if (progress.game.forecastable) round ! ForecastPlay(move)
      scheduleExpiration(progress.game)
      fuccess(progress.events)
    }
    res >>- promiseOption.foreach(_.success(()))
  }

  private[round] def draughtsnet(game: Game, uci: Uci, currentFen: FEN, round: RoundDuct, nextMove: Option[(Uci, String)] = None)(implicit proxy: GameProxy): Fu[Events] =
    if (game.playable && game.player.isAi) {
      if (currentFen == FEN(Forsyth >> game.draughts))
        applyUci(game, uci, blur = false, metrics = draughtsnetLag)
          .fold(errs => fufail(ClientError(errs.shows)), fuccess).flatMap {
            case Flagged => finisher.outOfTime(game)
            case MoveApplied(progress, move) =>
              proxy.save(progress) >>-
                uciMemo.add(progress.game, move) >>-
                notifyMove(move, progress.game) >> {
                  if (progress.game.finished) moveFinish(progress.game, game.turnColor) dmap { progress.events ::: _ }
                  else fuccess(progress.events)
                } >>- {
                  nextMove match {
                    case Some((uci, taken)) if !progress.game.finished =>
                      akka.pattern.after(captureDelay(game), system.scheduler) {
                        round ! DraughtsnetPlay(uci, taken, FEN(Forsyth >> progress.game.draughts))
                        funit
                      }
                    case _ =>
                  }
                }
          }
      else requestDraughtsnet(game, round) >> fufail(DraughtsnetError(s"Invalid AI move current FEN $currentFen != ${FEN(Forsyth >> game.draughts)}"))
    } else fufail(DraughtsnetError("Not AI turn"))

  private def requestDraughtsnet(game: Game, round: RoundDuct): Funit = game.playableByAi ?? {
    if (game.turns <= draughtsnetPlayer.maxPlies) draughtsnetPlayer(game)
    else fuccess(round ! actorApi.round.ResignAi)
  }

  private def toDelay(centis: Int) = approximatly(0.5f)(centis).milliseconds
  private def captureDelay(game: Game) = game.clock.fold(toDelay(400)) { clock =>
    val remaining = clock.remainingTime(game.player.color).centis
    if (remaining > 1500) toDelay(400)
    else if (remaining > 500) toDelay(200)
    else toDelay(100)
  }

  private val draughtsnetLag = MoveMetrics(clientLag = Centis(5).some)
  private val botLag = MoveMetrics(clientLag = Centis(10).some)

  private def applyUci(game: Game, uci: Uci, blur: Boolean, metrics: MoveMetrics, finalSquare: Boolean = false): Valid[MoveResult] =
    (uci match {
      case Uci.Move(orig, dest, prom, _) =>
        game.draughts(orig, dest, prom, metrics, finalSquare) map {
          case (ncg, move) => ncg -> move
        }
      case _ => s"Could not apply move: $uci".failureNel
    }).map {
      case (ncg, _) if ncg.clock.exists(_.outOfTime(game.turnColor, false)) => Flagged
      case (newGame, move) => MoveApplied(
        game.update(newGame, move, blur, metrics),
        move
      )
    }

  private def notifyMove(moveOrDrop: Move, game: Game): Unit = {
    import lidraughts.hub.actorApi.round.{ MoveEvent, CorresMoveEvent, SimulMoveEvent }

    val color = !moveOrDrop.situationAfter.color

    val moveEvent = MoveEvent(
      gameId = game.id,
      fen = Forsyth exportBoard game.board,
      move = moveOrDrop.toUci.keys
    )

    // publish all moves
    bus.publish(moveEvent, 'moveEvent)

    // I checked and the bus doesn't do much if there's no subscriber for a classifier,
    // so we should be good here.
    // also use for targeted TvBroadcast subscription
    bus.publish(MoveGameEvent makeBusEvent MoveGameEvent(game, moveEvent.fen, moveEvent.move))

    // publish correspondence moves
    if (game.isCorrespondence && game.nonAi) bus.publish(
      CorresMoveEvent(
        move = moveEvent,
        playerUserId = game.player(color).userId,
        mobilePushable = game.mobilePushable,
        alarmable = game.alarmable,
        unlimited = game.isUnlimited
      ),
      'moveEventCorres
    )

    // publish simul moves
    for {
      simulId <- game.simulId
      opponentUserId <- game.player(!color).userId
    } bus.publish(
      SimulMoveEvent(move = moveEvent, simulId = simulId, opponentUserId = opponentUserId),
      'moveEventSimul
    )

  }

  private def moveFinish(game: Game, color: Color)(implicit proxy: GameProxy): Fu[Events] =
    game.status match {
      case Status.Mate => finisher.other(game, _.Mate, game.situation.winner)
      case Status.VariantEnd => finisher.other(game, _.VariantEnd, game.situation.winner)
      case status @ (Status.Stalemate | Status.Draw) => finisher.other(game, _ => status, None)
      case _ => fuccess(Nil)
    }

}
