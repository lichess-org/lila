package lila.round

import scala.concurrent.Promise

import chess.format.{ Forsyth, FEN, Uci }
import chess.{ MoveMetrics, Centis, Status, Color, MoveOrDrop }

import actorApi.round.{ HumanPlay, DrawNo, TooManyPlies, TakebackNo, ForecastPlay }
import akka.actor.ActorRef
import lila.game.actorApi.MoveGameEvent
import lila.common.Bus
import lila.game.{ Game, GameDiff, Progress, Pov, UciMemo }
import lila.game.Game.{ PlayerId, FullId }
import lila.hub.actorApi.round.BotPlay

private[round] final class Player(
    fishnetPlayer: lila.fishnet.Player,
    finisher: Finisher,
    scheduleExpiration: Game => Unit,
    uciMemo: UciMemo
) {

  private sealed trait MoveResult
  private case object Flagged extends MoveResult
  private case class MoveApplied(progress: Progress, move: MoveOrDrop) extends MoveResult

  private[round] def human(play: HumanPlay, round: RoundDuct)(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = play match {
    case p @ HumanPlay(playerId, uci, blur, lag, _) => pov match {
      case Pov(game, _) if game.turns > Game.maxPlies =>
        round ! TooManyPlies
        fuccess(Nil)
      case Pov(game, color) if game playableBy color =>
        p.trace.segmentSync("applyUci", "logic")(applyUci(game, uci, blur, lag)).prefixFailuresWith(s"$pov ")
          .fold(errs => fufail(ClientError(errs.shows)), fuccess).flatMap {
            case Flagged => finisher.outOfTime(game)
            case MoveApplied(progress, moveOrDrop) =>
              p.trace.segment("save", "db")(proxy.save(progress)) >>
                postHumanOrBotPlay(round, pov, progress, moveOrDrop)
          }
      case Pov(game, _) if game.finished => fufail(ClientError(s"$pov game is finished"))
      case Pov(game, _) if game.aborted => fufail(ClientError(s"$pov game is aborted"))
      case Pov(game, color) if !game.turnOf(color) => fufail(ClientError(s"$pov not your turn"))
      case _ => fufail(ClientError(s"$pov move refused for some reason"))
    }
  }

  private[round] def bot(play: BotPlay, round: RoundDuct)(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = play match {
    case p @ BotPlay(playerId, uci, _) => pov match {
      case Pov(game, _) if game.turns > Game.maxPlies =>
        round ! TooManyPlies
        fuccess(Nil)
      case Pov(game, color) if game playableBy color =>
        applyUci(game, uci, false, botLag).prefixFailuresWith(s"$pov ")
          .fold(errs => fufail(ClientError(errs.shows)), fuccess).flatMap {
            case Flagged => finisher.outOfTime(game)
            case MoveApplied(progress, moveOrDrop) =>
              proxy.save(progress) >> postHumanOrBotPlay(round, pov, progress, moveOrDrop)
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
    moveOrDrop: MoveOrDrop
  )(implicit proxy: GameProxy): Fu[Events] = {
    if (pov.game.hasAi) uciMemo.add(pov.game, moveOrDrop)
    notifyMove(moveOrDrop, progress.game)
    if (progress.game.finished) moveFinish(progress.game, pov.color) dmap { progress.events ::: _ }
    else {
      if (progress.game.playableByAi) requestFishnet(progress.game, round)
      if (pov.opponent.isOfferingDraw) round ! DrawNo(PlayerId(pov.player.id))
      if (pov.player.isProposingTakeback) round ! TakebackNo(PlayerId(pov.player.id))
      if (progress.game.forecastable) moveOrDrop.left.toOption.foreach { move =>
        round ! ForecastPlay(move)
      }
      scheduleExpiration(progress.game)
      fuccess(progress.events)
    }
  }

  private[round] def fishnet(game: Game, ply: Int, uci: Uci, round: RoundDuct)(implicit proxy: GameProxy): Fu[Events] =
    if (game.playable && game.player.isAi && game.playedTurns == ply) {
      applyUci(game, uci, blur = false, metrics = fishnetLag)
        .fold(errs => fufail(ClientError(errs.shows)), fuccess).flatMap {
          case Flagged => finisher.outOfTime(game)
          case MoveApplied(progress, moveOrDrop) =>
            proxy.save(progress) >>-
              uciMemo.add(progress.game, moveOrDrop) >>-
              notifyMove(moveOrDrop, progress.game) >> {
                if (progress.game.finished) moveFinish(progress.game, game.turnColor) dmap { progress.events ::: _ }
                else fuccess(progress.events)
              }
        }
    } else fufail(FishnetError(s"Not AI turn move: ${uci} id: ${game.id} playable: ${game.playable} player: ${game.player}"))

  private[round] def requestFishnet(game: Game, round: RoundDuct): Funit = game.playableByAi ?? {
    if (game.turns <= fishnetPlayer.maxPlies) fishnetPlayer(game)
    else fuccess(round ! actorApi.round.ResignAi)
  }

  private val fishnetLag = MoveMetrics(clientLag = Centis(5).some)
  private val botLag = MoveMetrics(clientLag = Centis(10).some)

  private def applyUci(game: Game, uci: Uci, blur: Boolean, metrics: MoveMetrics): Valid[MoveResult] =
    (uci match {
      case Uci.Move(orig, dest, prom) => game.chess(orig, dest, prom, metrics) map {
        case (ncg, move) => ncg -> (Left(move): MoveOrDrop)
      }
      case Uci.Drop(role, pos) => game.chess.drop(role, pos, metrics) map {
        case (ncg, drop) => ncg -> (Right(drop): MoveOrDrop)
      }
    }).map {
      case (ncg, _) if ncg.clock.exists(_.outOfTime(game.turnColor, false)) => Flagged
      case (newChessGame, moveOrDrop) => MoveApplied(
        game.update(newChessGame, moveOrDrop, blur, metrics),
        moveOrDrop
      )
    }

  private def notifyMove(moveOrDrop: MoveOrDrop, game: Game): Unit = {
    import lila.hub.actorApi.round.{ MoveEvent, CorresMoveEvent, SimulMoveEvent }
    val color = moveOrDrop.fold(_.color, _.color)
    val moveEvent = MoveEvent(
      gameId = game.id,
      fen = Forsyth exportBoard game.board,
      move = moveOrDrop.fold(_.toUci.keys, _.toUci.uci)
    )

    // I checked and the bus doesn't do much if there's no subscriber for a classifier,
    // so we should be good here.
    // also used for targeted TvBroadcast subscription
    Bus.publish(MoveGameEvent makeBusEvent MoveGameEvent(game, moveEvent.fen, moveEvent.move))

    // publish correspondence moves
    if (game.isCorrespondence && game.nonAi) Bus.publish(
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
    } Bus.publish(
      SimulMoveEvent(move = moveEvent, simulId = simulId, opponentUserId = opponentUserId),
      'moveEventSimul
    )
  }

  private def moveFinish(game: Game, color: Color)(implicit proxy: GameProxy): Fu[Events] = game.status match {
    case Status.Mate => finisher.other(game, _.Mate, game.situation.winner)
    case Status.VariantEnd => finisher.other(game, _.VariantEnd, game.situation.winner)
    case status @ (Status.Stalemate | Status.Draw) => finisher.other(game, _ => status, None)
    case _ => fuccess(Nil)
  }
}
