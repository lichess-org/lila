package lidraughts.importer

import scala.concurrent.duration._

import akka.actor.ActorRef
import draughts.format.FEN
import draughts.{ Status, Situation }

import lidraughts.game.{ Game, GameRepo }

final class Importer(
    roundMap: ActorRef,
    delay: FiniteDuration,
    scheduler: akka.actor.Scheduler
) {

  def apply(data: ImportData, user: Option[String], forceId: Option[String] = None): Fu[Game] = {

    def gameExists(processing: => Fu[Game]): Fu[Game] =
      GameRepo.findPdnImport(data.pdn) flatMap { _.fold(processing)(fuccess) }

    def applyResult(game: Game, result: Result, situation: Situation): Game =
      if (game.finished) game
      else situation.status match {
        case Some(situationStatus) => game.finish(situationStatus, situation.winner).game
        case _ if result.status <= Status.Started => game
        case _ => game.finish(result.status, result.winner).game
      }

    gameExists {
      (data preprocess user).future flatMap {
        case Preprocessed(g, replay, result, initialFen, _) =>
          val game = applyResult(forceId.fold(g)(g.withId), result, replay.state.situation)
          (GameRepo.insertDenormalized(game, initialFen = initialFen)) >> {
            game.pdnImport.flatMap(_.user).isDefined ?? GameRepo.setImportCreatedAt(game)
          } >> {
            GameRepo.finish(
              id = game.id,
              winnerColor = game.winnerColor,
              winnerId = None,
              status = game.status
            )
          } inject game
      }
    }
  }

  def inMemory(data: ImportData): Valid[(Game, Option[FEN])] = data.preprocess(user = none).map {
    case Preprocessed(game, replay, _, fen, _) => (game withId "synthetic", fen)
  }
}
