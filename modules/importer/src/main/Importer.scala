package lidraughts.importer

import scala.concurrent.duration._

import akka.actor.ActorRef
import draughts.format.FEN
import draughts.{ Status, Situation }

import lidraughts.game.{ Game, GameRepo }

final class Importer(
    delay: FiniteDuration,
    scheduler: akka.actor.Scheduler
) {

  def apply(data: ImportData, user: Option[String], forceId: Option[String] = None): Fu[Game] = {

    def gameExists(processing: => Fu[Game]): Fu[Game] =
      GameRepo.findPdnImport(data.pdn) flatMap { _.fold(processing)(fuccess) }

    gameExists {
      (data preprocess user).future flatMap {
        case Preprocessed(g, replay, initialFen, _) =>
          val game = forceId.fold(g.sloppy)(g.withId)
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
    case Preprocessed(game, replay, fen, _) => (game withId "synthetic", fen)
  }
}
