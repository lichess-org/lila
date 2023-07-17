package lila.explorer

import lila.game.{ Game, GameRepo }
import lila.importer.{ ImportData, Importer }
import play.api.libs.ws.DefaultBodyReadables.*
import chess.format.pgn.PgnStr

final class ExplorerImporter(
    endpoint: InternalEndpoint,
    gameRepo: GameRepo,
    gameImporter: Importer,
    ws: play.api.libs.ws.StandaloneWSClient
)(using Executor):

  private val masterGameEncodingFixedAt = instantOf(2016, 3, 9, 0, 0)

  def apply(id: GameId): Fu[Option[Game]] =
    gameRepo game id flatMap {
      case Some(game) if !game.isPgnImport || game.createdAt.isAfter(masterGameEncodingFixedAt) =>
        fuccess(game.some)
      case _ =>
        gameRepo.remove(id) >> fetchPgn(id).flatMapz { pgn =>
          gameImporter(
            ImportData(pgn, none),
            forceId = id.some
          )(using lila.user.User.lichessIdAsMe.some) map some
        }
    }

  private def fetchPgn(id: GameId): Fu[Option[PgnStr]] =
    ws.url(s"$endpoint/masters/pgn/$id").get() map {
      case res if res.status == 200 => PgnStr from res.body[String].some
      case _                        => None
    }
