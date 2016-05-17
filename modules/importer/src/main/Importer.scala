package lila.importer

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.ActorRef
import akka.pattern.{ ask, after }
import chess.{ Color, MoveOrDrop, Status, Situation }
import chess.format.FEN
import makeTimeout.large

import lila.db.dsl._
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._

final class Importer(
    roundMap: ActorRef,
    delay: FiniteDuration,
    scheduler: akka.actor.Scheduler) {

  def apply(data: ImportData, user: Option[String], forceId: Option[String] = None): Fu[Game] = {

    def gameExists(processing: => Fu[Game]): Fu[Game] =
      GameRepo.findPgnImport(data.pgn) flatMap { _.fold(processing)(fuccess) }

    def applyResult(game: Game, result: Option[Result], situation: Situation): Game =
      if (game.finished) game
      else situation.status match {
        case Some(status) => game.finish(status, situation.winner).game
        case _ => result.fold(game) {
          case Result(Status.Draw, _)               => game.finish(Status.Draw, None).game
          case Result(Status.Resign, winner)        => game.finish(Status.Resign, winner).game
          case Result(Status.UnknownFinish, winner) => game.finish(Status.UnknownFinish, winner).game
          case _                                    => game
        }
      }

    gameExists {
      (data preprocess user).future flatMap {
        case Preprocessed(g, replay, result, initialFen, _) =>
          val started = forceId.fold(g)(g.withId).start
          val game = applyResult(started, result, replay.state.situation)
          (GameRepo.insertDenormalized(game, initialFen = initialFen)) >> {
            game.pgnImport.flatMap(_.user).isDefined ?? GameRepo.setImportCreatedAt(game)
          } >> {
            GameRepo.finish(
              id = game.id,
              winnerColor = game.winnerColor,
              winnerId = None,
              status = game.status)
          } inject game
      }
    }
  }

  def inMemory(data: ImportData): Valid[(Game, Option[FEN])] = data.preprocess(user = none).map {
    case Preprocessed(game, replay, _, fen, _) => (game withId "synthetic", fen)
  }
}
