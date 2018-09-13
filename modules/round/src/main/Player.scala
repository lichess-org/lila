package lidraughts.round

import scala.concurrent.duration._
import draughts.format.{ FEN, Forsyth, Uci }
import draughts.{ Centis, Color, Move, MoveMetrics, Status }
import actorApi.round.{ DrawNo, ForecastPlay, HumanPlay, TakebackNo, TooManyPlies }
import akka.actor._
import lidraughts.game.{ Game, Pov, Progress, UciMemo }
import lidraughts.hub.actorApi.round.DraughtsnetPlay

private[round] final class Player(
    draughtsnetPlayer: lidraughts.draughtsnet.Player,
    bus: lidraughts.common.Bus,
    finisher: Finisher,
    uciMemo: UciMemo
) {

  private sealed trait MoveResult
  private case object Flagged extends MoveResult
  private case class MoveApplied(progress: Progress, move: Move) extends MoveResult

  def human(play: HumanPlay, round: ActorRef)(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = play match {
    case p @ HumanPlay(playerId, uci, blur, lag, promiseOption, finalSquare) => pov match {
      case Pov(game, _) if game.turns > Game.maxPlies =>
        round ! TooManyPlies
        fuccess(Nil)
      case Pov(game, color) if game playableBy color =>
        p.trace.segmentSync("applyUci", "logic")(applyUci(game, uci, blur, lag, finalSquare)).prefixFailuresWith(s"$pov ")
          .fold(errs => fufail(ClientError(errs.shows)), fuccess).flatMap {
            case Flagged => finisher.outOfTime(game)
            case MoveApplied(progress, moveOrDrop) => p.trace.segment("save", "db")(proxy save progress) >>- {
              if (pov.game.hasAi) uciMemo.add(pov.game, moveOrDrop)
              notifyMove(moveOrDrop, progress.game)
            } >> progress.game.finished.fold(
              moveFinish(progress.game, color) dmap { progress.events ::: _ }, {
                if (progress.game.playableByAi) requestDraughtsnet(progress.game, round)
                if (pov.opponent.isOfferingDraw) round ! DrawNo(pov.player.id)
                if (pov.player.isProposingTakeback) round ! TakebackNo(pov.player.id)
                if (pov.game.forecastable) round ! ForecastPlay(moveOrDrop)
                fuccess(progress.events)
              }
            ) >>- promiseOption.foreach(_.success(()))
          } addFailureEffect { e =>
            promiseOption.foreach(_ failure e)
          }
      case Pov(game, _) if game.finished => fufail(ClientError(s"$pov game is finished"))
      case Pov(game, _) if game.aborted => fufail(ClientError(s"$pov game is aborted"))
      case Pov(game, color) if !game.turnOf(color) => fufail(ClientError(s"$pov not your turn"))
      case _ => fufail(ClientError(s"$pov move refused for some reason"))
    }
  }

  def draughtsnet(game: Game, uci: Uci, currentFen: FEN, round: ActorRef, context: ActorContext, nextMove: Option[(Uci, String)] = None)(implicit proxy: GameProxy): Fu[Events] =
    if (game.playable && game.player.isAi) {
      if (currentFen == FEN(Forsyth >> game.draughts))
        applyUci(game, uci, blur = false, metrics = draughtsnetLag)
          .fold(errs => fufail(ClientError(errs.shows)), fuccess).flatMap {
            case Flagged => finisher.outOfTime(game)
            case MoveApplied(progress, move) =>
              proxy.save(progress) >>-
                uciMemo.add(progress.game, move) >>-
                notifyMove(move, progress.game) >>
                progress.game.finished.fold(
                  moveFinish(progress.game, game.turnColor) dmap { progress.events ::: _ },
                  fuccess(progress.events)
                ) >>-
                  nextMove.fold() { nextMove =>
                    context.system.scheduler.scheduleOnce(game.clock.fold((300 + scala.util.Random.nextInt(200)).milliseconds) {
                      clock =>
                        val remaining = clock.remainingTime(game.player.color).centis
                        if (remaining > 1500)
                          (300 + scala.util.Random.nextInt(200)).milliseconds
                        else if (remaining > 500)
                          (200 + scala.util.Random.nextInt(100)).milliseconds
                        else
                          (100 + scala.util.Random.nextInt(50)).milliseconds
                    }, round, DraughtsnetPlay(nextMove._1, nextMove._2, FEN(Forsyth >> progress.game.draughts)))
                  }
          }
      else requestDraughtsnet(game, round) >> fufail(DraughtsnetError(s"Invalid AI move current FEN $currentFen != ${FEN(Forsyth >> game.draughts)}"))
    } else fufail(DraughtsnetError("Not AI turn"))

  private def requestDraughtsnet(game: Game, round: ActorRef): Funit = game.playableByAi ?? {
    if (game.turns <= draughtsnetPlayer.maxPlies) draughtsnetPlayer(game)
    else fuccess(round ! actorApi.round.ResignAi)
  }

  private val draughtsnetLag = MoveMetrics(clientLag = Centis(5).some)

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
