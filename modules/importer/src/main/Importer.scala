package lila.importer

import chess.format.FEN

import lila.game.{ Game, GameRepo }

final class Importer(gameRepo: GameRepo)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(data: ImportData, user: Option[String], forceId: Option[String] = None): Fu[Game] = {

    def gameExists(processing: => Fu[Game]): Fu[Game] =
      gameRepo.findPgnImport(data.pgn) flatMap { _.fold(processing)(fuccess) }

    gameExists {
      (data preprocess user).future flatMap {
        case Preprocessed(g, _, initialFen, _) =>
          val game = forceId.fold(g.sloppy)(g.withId)
          (gameRepo.insertDenormalized(game, initialFen = initialFen)) >> {
            game.pgnImport.flatMap(_.user).isDefined ?? gameRepo.setImportCreatedAt(game)
          } >> {
            gameRepo.finish(
              id = game.id,
              winnerColor = game.winnerColor,
              winnerId = None,
              status = game.status
            )
          } inject game
      }
    }
  }

  def inMemory(data: ImportData): Valid[(Game, Option[FEN])] =
    data.preprocess(user = none).map {
      case Preprocessed(game, _, fen, _) => (game withId "synthetic", fen)
    }
}
