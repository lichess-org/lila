package lila.opening

import chess.format.FEN
import com.softwaremill.tagging._
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.ExecutionContext

final private class OpeningExplorer(
    ws: StandaloneWSClient,
    explorerEndpoint: String @@ ExplorerEndpoint
)(implicit ec: ExecutionContext) {
  import OpeningExplorer._

  def apply(query: OpeningQuery): Fu[Option[Position]] =
    ws.url(s"$explorerEndpoint/lichess")
      .withQueryStringParameters(
        "fen"         -> query.fen.value,
        "moves"       -> "10",
        "recentGames" -> "0"
      )
      .get()
      .flatMap {
        case res if res.status == 404 => fuccess(none)
        case res if res.status != 200 =>
          fufail(s"Couldn't reach the opening explorer: $query")
        case res =>
          res
            .body[JsValue]
            .validate[Position](positionReads)
            .fold(
              err => fufail(s"Couldn't parse opening data for $query: $err"),
              data => fuccess(data.some)
            )
      }
}

object OpeningExplorer {

  case class Position(
      white: Int,
      draws: Int,
      black: Int,
      moves: List[Move]
  )

  case class Move(uci: String, san: String, averageRating: Int, white: Int, draws: Int, black: Int)

  implicit private val moveReads     = Json.reads[Move]
  implicit private val positionReads = Json.reads[Position]
}
