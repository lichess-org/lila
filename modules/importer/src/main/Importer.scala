package lila.importer

import lila.game.GameRepo

final class Importer(gameRepo: GameRepo)(using Executor):

  def apply(data: ImportData, forceId: Option[GameId] = None)(using me: Option[MyId]): Fu[Game] =
    gameRepo
      .findPgnImport(data.pgn)
      .flatMap:
        case Some(game) => fuccess(game)
        case None =>
          (data.preprocess(me)).toFuture.flatMap { case Preprocessed(g, _, initialFen, _) =>
            val game = forceId.fold(g.sloppy)(g.withId)
            (gameRepo.insertDenormalized(game, initialFen = initialFen) >> {
              game.pgnImport.flatMap(_.user).isDefined.so(gameRepo.setImportCreatedAt(game))
            } >> {
              gameRepo.finish(
                id = game.id,
                winnerColor = game.winnerColor,
                winnerId = None,
                status = game.status
              )
            }).inject(game)
          }
