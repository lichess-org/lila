package lila.importer

import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern.ask
import chess.{ Color, Move, Status }
import makeTimeout.large

import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._

private[importer] final class Importer(
    roundMap: ActorRef,
    bookmark: akka.actor.ActorSelection,
    delay: Duration) {

  def apply(data: ImportData, user: Option[String], ip: String): Fu[Game] = {

    def gameExists(processing: => Fu[Game]): Fu[Game] =
      $find.one(lila.game.Query pgnImport data.pgn) flatMap { _.fold(processing)(fuccess) }

    def applyResult(game: Game, result: Result) {
      result match {
        case Result(Status.Draw, _)             => roundMap ! Tell(game.id, DrawForce)
        case Result(Status.Resign, Some(color)) => roundMap ! Tell(game.id, Resign(game.player(!color).id))
        case _                                  =>
      }
    }

    def applyMoves(pov: Pov, moves: List[Move]) {
      moves match {
        case move :: rest => {
          applyMove(pov, move)
          Thread sleep delay.toMillis
          applyMoves(!pov, rest)
        }
        case Nil =>
      }
    }

    def applyMove(pov: Pov, move: Move) {
      roundMap ! Tell(pov.gameId, HumanPlay(
        playerId = pov.playerId,
        ip = ip,
        orig = move.orig.toString,
        dest = move.dest.toString,
        prom = move.promotion map (_.name),
        blur = false,
        lag = 0.millis,
        onFailure = _ => ()
      ))
    }

    gameExists {
      (data preprocess user).future flatMap {
        case Preprocessed(game, moves, result) =>
          (GameRepo insertDenormalized game) >>-
            applyMoves(Pov(game, Color.white), moves) >>-
            (result foreach { r => applyResult(game, r) }) inject game
      }
    }
  }
}
