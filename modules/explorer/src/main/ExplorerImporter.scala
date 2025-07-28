package lila.explorer

import chess.format.pgn.PgnStr
import play.api.libs.ws.DefaultBodyReadables.*

final class ExplorerImporter(
    endpoint: InternalEndpoint,
    gameRepo: lila.core.game.GameRepo,
    gameImporter: lila.game.importer.Importer,
    ws: play.api.libs.ws.StandaloneWSClient
)(using Executor)
    extends lila.core.game.Explorer:

  private val masterGameEncodingFixedAt = instantOf(2016, 3, 9, 0, 0)

  def apply(id: GameId): Fu[Option[Game]] =
    gameRepo
      .game(id)
      .flatMap:
        case Some(game) if !game.isPgnImport || game.createdAt.isAfter(masterGameEncodingFixedAt) =>
          fuccess(game.some)
        case _ =>
          for
            _ <- gameRepo.remove(id)
            pgn <- fetchPgn(id)
            game <- pgn.so: pgn =>
              gameImporter
                .importAsGame(
                  pgn,
                  id.some
                )(using UserId.lichessAsMe.some)
                .map(some)
          yield game

  private def fetchPgn(id: GameId): Fu[Option[PgnStr]] =
    ws.url(s"$endpoint/masters/pgn/$id")
      .get()
      .map:
        case res if res.status == 200 => PgnStr.from(res.body[String].some)
        case _ => None
