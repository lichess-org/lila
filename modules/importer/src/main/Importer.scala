package lila.importer

import chess.{ Color, Move, Status }
import lila.game.{ Game, GameRepo, Pov }
import lila.round.actorApi.round._
import lila.hub.actorApi.Tell
import lila.game.tube.gameTube
import lila.db.api._
import makeTimeout.large

import akka.actor.ActorRef
import akka.pattern.ask
import scala.concurrent.duration.Duration

private[importer] final class Importer(
    roundMap: ActorRef,
    bookmark: lila.hub.ActorLazyRef,
    delay: Duration) {

  def apply(data: ImportData, user: Option[String]): Fu[Game] = gameExists(data.pgn) {
    (data preprocess user).future flatMap {
      case Preprocessed(game, moves, result) ⇒
        (GameRepo insertDenormalized game) >>-
          applyMoves(Pov(game, Color.white), moves) >>-
          (result foreach { r ⇒ applyResult(game, r) }) inject game
    }
  }

  private def gameExists(pgn: String)(processing: ⇒ Fu[Game]): Fu[Game] =
    $find.one(lila.game.Query pgnImport pgn) flatMap {
      _.fold(processing)(game ⇒ fuccess(game))
    }

  private def applyResult(game: Game, result: Result) {
    result match {
      case Result(Status.Draw, _)             ⇒ roundMap ! Tell(game.id, DrawForce)
      case Result(Status.Resign, Some(color)) ⇒ roundMap ! Tell(game.id, Resign(game.player(!color).id))
      case _                                  ⇒
    }
  }

  private def applyMoves(pov: Pov, moves: List[Move]) {
    moves match {
      case move :: rest ⇒ {
        applyMove(pov, move)
        Thread sleep delay.toMillis
        applyMoves(!pov, rest)
      }
      case Nil ⇒
    }
  }

  private def applyMove(pov: Pov, move: Move) {
    roundMap ! Tell(pov.gameId, HumanPlay(
      playerId = pov.playerId,
      orig = move.orig.toString,
      dest = move.dest.toString,
      prom = move.promotion map (_.forsyth.toString),
      blur = false,
      lag = 0,
      onFailure = _ ⇒ ()
    ))
  }
}
