package lila.round

import chess.format.{ Forsyth, FEN, Uci }
import chess.{ Centis, Status, Color, MoveOrDrop }

import actorApi.round.{ HumanPlay, DrawNo, TakebackNo, ForecastPlay }
import akka.actor.ActorRef
import lila.game.{ Game, Pov, UciMemo }
import lila.hub.actorApi.round.MoveEvent
import scala.concurrent.duration._

private[round] final class Player(
    fishnetPlayer: lila.fishnet.Player,
    bus: lila.common.Bus,
    finisher: Finisher,
    uciMemo: UciMemo
) {

  def human(play: HumanPlay, round: ActorRef)(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = play match {
    case p @ HumanPlay(playerId, uci, blur, lag, promiseOption) => pov match {
      case Pov(game, color) if game playableBy color =>
        p.trace.segmentSync("applyUci", "logic")(applyUci(game, uci, blur, lag + humanLag)).prefixFailuresWith(s"$pov ")
          .fold(errs => fufail(ClientError(errs.shows)), fuccess).flatMap {
            case (progress, moveOrDrop) =>
              p.trace.segment("save", "db")(proxy save progress) >>- {
                if (pov.game.hasAi) uciMemo.add(pov.game, moveOrDrop)
                notifyMove(moveOrDrop, progress.game)
              } >> progress.game.finished.fold(
                moveFinish(progress.game, color) dmap { progress.events ::: _ }, {
                  if (progress.game.playableByAi) requestFishnet(progress.game, round)
                  if (pov.opponent.isOfferingDraw) round ! DrawNo(pov.player.id)
                  if (pov.player.isProposingTakeback) round ! TakebackNo(pov.player.id)
                  if (pov.game.forecastable) moveOrDrop.left.toOption.foreach { move =>
                    round ! ForecastPlay(move)
                  }
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

  def fishnet(game: Game, uci: Uci, currentFen: FEN, round: ActorRef)(implicit proxy: GameProxy): Fu[Events] =
    if (game.playable && game.player.isAi) {
      if (currentFen == FEN(Forsyth >> game.toChess))
        if (game.outoftime(_ => fishnetLag)) finisher.outOfTime(game)
        else applyUci(game, uci, blur = false, lag = fishnetLag)
          .fold(errs => fufail(ClientError(errs.shows)), fuccess).flatMap {
            case (progress, moveOrDrop) =>
              proxy.save(progress) >>-
                uciMemo.add(progress.game, moveOrDrop) >>-
                notifyMove(moveOrDrop, progress.game) >>
                progress.game.finished.fold(
                  moveFinish(progress.game, game.turnColor) dmap { progress.events ::: _ },
                  fuccess(progress.events)
                )
          }
      else requestFishnet(game, round) >> fufail(FishnetError("Invalid AI move current FEN"))
    }
    else fufail(FishnetError("Not AI turn"))

  private def requestFishnet(game: Game, round: ActorRef): Funit = game.playableByAi ?? {
    if (game.turns <= fishnetPlayer.maxPlies) fishnetPlayer(game)
    else fuccess(round ! actorApi.round.ResignAi)
  }

  private val clientLag = Centis(3)
  private val serverLag = Centis(1)
  private val humanLag = clientLag + serverLag
  private val fishnetLag = Centis(1) + serverLag

  private def applyUci(game: Game, uci: Uci, blur: Boolean, lag: Centis) = (uci match {
    case Uci.Move(orig, dest, prom) => game.toChess.apply(orig, dest, prom, lag) map {
      case (ncg, move) => ncg -> (Left(move): MoveOrDrop)
    }
    case Uci.Drop(role, pos) => game.toChess.drop(role, pos, lag) map {
      case (ncg, drop) => ncg -> (Right(drop): MoveOrDrop)
    }
  }).map {
    case (newChessGame, moveOrDrop) =>
      game.update(newChessGame, moveOrDrop, blur, lag.some) -> moveOrDrop
  }

  private def notifyMove(moveOrDrop: MoveOrDrop, game: Game) {
    val color = moveOrDrop.fold(_.color, _.color)
    bus.publish(MoveEvent(
      gameId = game.id,
      fen = Forsyth exportBoard game.toChess.board,
      move = moveOrDrop.fold(_.toUci.keys, _.toUci.uci),
      mobilePushable = game.mobilePushable,
      alarmable = game.alarmable,
      opponentUserId = game.player(!color).userId,
      simulId = game.simulId
    ), 'moveEvent)
  }

  private def moveFinish(game: Game, color: Color)(implicit proxy: GameProxy): Fu[Events] = game.status match {
    case Status.Mate => finisher.other(game, _.Mate, game.toChess.situation.winner)
    case Status.VariantEnd => finisher.other(game, _.VariantEnd, game.toChess.situation.winner)
    case status @ (Status.Stalemate | Status.Draw) => finisher.other(game, _ => status)
    case _ => fuccess(Nil)
  }
}
