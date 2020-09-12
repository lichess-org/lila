package lila.explorer

import org.joda.time.DateTime

import lila.game.{ Game, GameRepo }
import lila.importer.{ ImportData, Importer }

final class ExplorerImporter(
    endpoint: InternalEndpoint,
    gameRepo: GameRepo,
    gameImporter: Importer,
    ws: play.api.libs.ws.StandaloneWSClient
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val masterGameEncodingFixedAt = new DateTime(2016, 3, 9, 0, 0)

  def apply(id: Game.ID): Fu[Option[Game]] =
    gameRepo game id flatMap {
      case Some(game) if !game.isPgnImport || game.createdAt.isAfter(masterGameEncodingFixedAt) =>
        fuccess(game.some)
      case _ =>
        (gameRepo remove id) >> fetchPgn(id) flatMap {
          case None => fuccess(none)
          case Some(pgn) =>
            gameImporter(
              ImportData(pgn, none),
              user = "lichess".some,
              forceId = id.some
            ) map some
        }
    }

  private def fetchPgn(id: String): Fu[Option[String]] = {
    ws.url(s"$endpoint/master/pgn/$id").get() map {
      case res if res.status == 200 => res.body.some
      case _                        => None
    }
  }
}
