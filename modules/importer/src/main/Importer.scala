package lila.importer

import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern.{ ask, after }
import chess.{ Color, Move, Status }
import makeTimeout.large

import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._

final class Importer(
    roundMap: ActorRef,
    delay: FiniteDuration,
    scheduler: akka.actor.Scheduler) {

  def apply(data: ImportData, user: Option[String], ip: String): Fu[Game] = {

    def gameExists(processing: => Fu[Game]): Fu[Game] =
      $find.one(lila.game.Query pgnImport data.pgn) flatMap { _.fold(processing)(fuccess) }

    gameExists {
      doImport(data, user, ip)
    }
  }

  def doImport(data: ImportData, user: Option[String], ip: String): Fu[Game] = {

    def applyMoves(pov: Pov, moves: List[Move]): Funit = moves match {
      case Nil => after(delay, scheduler)(funit)
      case move :: rest =>
        after(delay, scheduler)(applyMove(pov, move)) >> applyMoves(!pov, rest)
    }

    def applyMove(pov: Pov, move: Move) = scala.concurrent.Future {
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

      (data preprocess user).future flatMap {
        case Preprocessed(game, moves, result) =>
          (GameRepo insertDenormalized game) >> {
            game.pgnImport.flatMap(_.user).isDefined ?? GameRepo.setImportCreatedAt(game)
        } >>
          applyMoves(Pov(game, Color.white), moves) >>-
            (result foreach { r => applyResult(game, r) }) inject game
      }
  }

  def applyResult(game: Game, result: Result) {
    result match {
      case Result(Status.Draw, _)             => roundMap ! Tell(game.id, DrawForce)
      case Result(Status.Resign, Some(color)) => roundMap ! Tell(game.id, Resign(game.player(!color).id))
      case _                                  =>
    }
  }
}
