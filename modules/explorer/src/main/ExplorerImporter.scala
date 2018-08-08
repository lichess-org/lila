package lidraughts.explorer

import org.joda.time.DateTime

import lidraughts.game.{ Game, GameRepo }
import lidraughts.importer.{ Importer, ImportData }

final class ExplorerImporter(
    endpoint: String,
    gameImporter: Importer
) {

  private val masterGameEncodingFixedAt = new DateTime(2016, 3, 9, 0, 0)

  def apply(id: Game.ID): Fu[Option[Game]] =
    GameRepo game id flatMap {
      case Some(game) if !game.isPdnImport || game.createdAt.isAfter(masterGameEncodingFixedAt) => fuccess(game.some)
      case _ => (GameRepo remove id) >> fetchPdn(id) flatMap {
        case None => fuccess(none)
        case Some(pdn) => gameImporter(
          ImportData(pdn, none),
          user = "lidraughts".some,
          forceId = id.some
        ) map some
      }
    }

  private def fetchPdn(id: String): Fu[Option[String]] = {
    import play.api.libs.ws.WS
    import play.api.Play.current
    WS.url(s"$endpoint/master/pdn/$id").get() map {
      case res if res.status == 200 => res.body.some
      case _ => None
    }
  }
}
