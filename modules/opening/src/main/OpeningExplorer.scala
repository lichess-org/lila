package lila.opening

import chess.format.Uci
import chess.format.pgn.SanStr
import chess.opening.Opening
import chess.IntRating
import com.softwaremill.tagging.*
import play.api.libs.json.{ JsObject, JsValue, Json, Reads }
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import scala.util.{ Failure, Success, Try }

import lila.core.net.Crawler

final private class OpeningExplorer(
    ws: StandaloneWSClient,
    explorerEndpoint: String @@ ExplorerEndpoint
)(using Executor):
  import OpeningExplorer.*

  private val requestTimeout = 4.seconds

  // weird looking return type, but it was convenient here
  def stats(play: Vector[Uci], config: OpeningConfig, crawler: Crawler): Fu[Try[Option[Position]]] =
    ws.url(s"$explorerEndpoint/lichess")
      .withQueryStringParameters(
        "since" -> OpeningQuery.firstMonth,
        "play" -> play.map(_.uci).mkString(","),
        "ratings" -> config.ratings.mkString(","),
        "speeds" -> config.speeds.map(_.key).mkString(","),
        "history" -> "yes",
        "source" -> (if crawler.yes then "openingCrawler" else "opening")
      )
      .withRequestTimeout(requestTimeout)
      .get()
      .flatMap:
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
      .monSuccess(_.opening.explorer.stats)
      .map(Success(_))
      .recover:
        case e: Exception =>
          logger.warn(s"Opening stats $play $config", e)
          Failure(e)

  def simplePopularity(opening: Opening): Fu[Option[Long]] =
    ws.url(s"$explorerEndpoint/lichess")
      .withQueryStringParameters(
        "since" -> OpeningQuery.firstMonth,
        "play" -> opening.uci.value.replace(" ", ","),
        "moves" -> "0",
        "topGames" -> "0",
        "recentGames" -> "0",
        "source" -> "opening"
      )
      .withRequestTimeout(requestTimeout)
      .get()
      .flatMap:
        case res if res.status == 404 => fuccess(none)
        case res if res.status != 200 =>
          fufail(s"Couldn't reach the opening explorer: ${res.status}")
        case res =>
          res
            .body[JsValue]
            .validate[Stats]
            .fold(
              err => fufail(s"Couldn't parse $err"),
              data => fuccess(data.sum.some)
            )
      .recover { case e: Exception =>
        logger.warn(s"Opening simple popularity $opening", e)
        none
      }

object OpeningExplorer:

  case class Position(
      white: Long,
      draws: Long,
      black: Long,
      moves: List[Move],
      topGames: List[GameRef],
      recentGames: List[GameRef],
      history: List[Stats]
  ):
    val sum = white + draws + black
    val movesSum = moves.foldLeft(0L)(_ + _.sum)
    val games = topGames ::: recentGames

    def popularityHistory: PopularityHistoryAbsolute =
      history.map(_.sum)

  case class Move(uci: String, san: SanStr, averageRating: IntRating, white: Long, draws: Long, black: Long):
    def sum = white + draws + black

  case class GameRef(id: GameId)

  case class Stats(
      white: Long,
      draws: Long,
      black: Long
  ):
    def sum = white + draws + black

  import lila.common.Json.given
  private given Reads[Move] = Json.reads
  private given Reads[GameRef] = Json.reads
  private given Reads[Position] = Json.reads
  private given Reads[Stats] = Json.reads
