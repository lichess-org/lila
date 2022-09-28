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
        "ratings"     -> query.config.ratings.mkString(","),
        "speeds"      -> query.config.speeds.map(_.key).mkString(","),
        "fen"         -> query.fen.value,
        "moves"       -> "12",
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
      white: Long,
      draws: Long,
      black: Long,
      moves: List[Move]
  ) {
    val movesSum = moves.foldLeft(0L)(_ + _.sum)
  }

  case class Move(uci: String, san: String, averageRating: Int, white: Int, draws: Int, black: Int) {
    def sum = white + draws + black
  }

  implicit private val moveReads     = Json.reads[Move]
  implicit private val positionReads = Json.reads[Position]
}
