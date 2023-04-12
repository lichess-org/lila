package lila.opening

import chess.format.pgn.SanStr
import chess.opening.Opening
import com.softwaremill.tagging.*
import play.api.libs.json.{ JsObject, JsValue, Json, Reads }
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

final private class OpeningExplorer(
    ws: StandaloneWSClient,
    explorerEndpoint: String @@ ExplorerEndpoint
)(using Executor):
  import OpeningExplorer.*

  private val requestTimeout = 4.seconds

  def stats(query: OpeningQuery): Fu[Option[Position]] =
    ws.url(s"$explorerEndpoint/lichess")
      .withQueryStringParameters(queryParameters(query)*)
      .withRequestTimeout(requestTimeout)
      .get()
      .flatMap {
        case res if res.status == 404 => fuccess(none)
        case res if res.status != 200 =>
          fufail(s"Couldn't reach the opening explorer: ${res.status}")
        case res =>
          res
            .body[JsValue]
            .validate[Position]
            .fold(
              err => fufail(s"Couldn't parse $err"),
              data => fuccess(data.some)
            )
      }
      .recover { case e: Exception =>
        logger.warn(s"Opening stats $query", e)
        none
      }

  def queryHistory(query: OpeningQuery): Fu[PopularityHistoryAbsolute] =
    historyOf(queryParameters(query))

  def configHistory(config: OpeningConfig): Fu[PopularityHistoryAbsolute] =
    historyOf(configParameters(config))

  def simplePopularity(opening: Opening): Fu[Option[Long]] =
    ws.url(s"$explorerEndpoint/lichess")
      .withQueryStringParameters(
        "play"        -> opening.uci.value.replace(" ", ","),
        "moves"       -> "0",
        "topGames"    -> "0",
        "recentGames" -> "0"
      )
      .withRequestTimeout(requestTimeout)
      .get()
      .flatMap {
        case res if res.status == 404 => fuccess(none)
        case res if res.status != 200 =>
          fufail(s"Couldn't reach the opening explorer: ${res.status}")
        case res =>
          res
            .body[JsValue]
            .validate[Position]
            .fold(
              err => fufail(s"Couldn't parse $err"),
              data => fuccess(data.sum.some)
            )
      }
      .recover { case e: Exception =>
        logger.warn(s"Opening simple popularity $opening", e)
        none
      }

  private val dateFormat = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")

  private def historyOf(params: List[(String, String)]): Fu[PopularityHistoryAbsolute] =
    ws.url(s"$explorerEndpoint/lichess/history")
      .withQueryStringParameters(
        params ::: List(
          "until" -> dateFormat.print(nowInstant.minusDays(45))
        )*
      )
      .withRequestTimeout(requestTimeout)
      .get()
      .flatMap {
        case res if res.status != 200 =>
          fufail(s"Opening explorer: ${res.status}")
        case res =>
          (res.body[JsValue] \ "history")
            .validate[List[JsObject]]
            .fold(
              invalid => fufail(invalid.toString),
              months =>
                fuccess {
                  months.map { o =>
                    ~o.int("black") + ~o.int("draws") + ~o.int("white")
                  }
                }
            )
      }
      .recover { case e: Exception =>
        logger.warn(s"Opening history $params", e)
        Nil
      }

  private def queryParameters(query: OpeningQuery) =
    configParameters(query.config) ::: List(
      "play" -> query.uci.map(_.uci).mkString(",")
    )
  private def configParameters(config: OpeningConfig) =
    List(
      "since"   -> OpeningQuery.firstMonth,
      "ratings" -> config.ratings.mkString(","),
      "speeds"  -> config.speeds.map(_.key).mkString(",")
    )

object OpeningExplorer:

  case class Position(
      white: Long,
      draws: Long,
      black: Long,
      moves: List[Move],
      topGames: List[GameRef],
      recentGames: List[GameRef]
  ):
    val sum      = white + draws + black
    val movesSum = moves.foldLeft(0L)(_ + _.sum)
    val games    = topGames ::: recentGames

  case class Move(uci: String, san: SanStr, averageRating: IntRating, white: Long, draws: Long, black: Long):
    def sum = white + draws + black

  case class GameRef(id: GameId)

  import lila.common.Json.given
  private given Reads[Move]     = Json.reads
  private given Reads[GameRef]  = Json.reads
  private given Reads[Position] = Json.reads
