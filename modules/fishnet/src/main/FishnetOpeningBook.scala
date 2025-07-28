package lila.fishnet

import chess.format.{ Fen, Uci }
import chess.{ Color, Speed }
import com.softwaremill.tagging.*
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import scalalib.ThreadLocalRandom

import lila.common.Json.given
import lila.memo.SettingStore

final private class FishnetOpeningBook(
    ws: StandaloneWSClient,
    depth: SettingStore[Int] @@ FishnetOpeningBook.Depth,
    config: FishnetConfig
)(using Executor):

  import FishnetOpeningBook.{ *, given }

  private val outOfBook = scalalib.cache.ExpireSetMemo[GameId](10.minutes)

  def apply(game: Game, level: Int): Fu[Option[Uci]] =
    (game.ply < depth.get() && !outOfBook.get(game.id)).so:
      ws.url(s"${config.explorerEndpoint}/lichess")
        .withRequestTimeout(800.millis)
        .withQueryStringParameters(
          "variant" -> game.variant.key.value,
          "fen" -> Fen.write(game.chess).value,
          "topGames" -> "0",
          "recentGames" -> "0",
          "ratings" -> (~levelRatings.get(level)).mkString(","),
          "speeds" -> (~openingSpeeds.get(game.speed)).map(_.key).mkString(","),
          "source" -> "fishnet"
        )
        .get()
        .map:
          case res if res.status != 200 =>
            logger.warn(s"opening book ${game.id} ${level} ${res.status} ${res.body}")
            none
          case res =>
            for
              data <- res.body[JsValue].validate[Response].asOpt
              _ = if data.moves.isEmpty then outOfBook.put(game.id)
              move <- data.randomPonderedMove(game.turnColor, level)
            yield move.uci
        .recover { case _: java.util.concurrent.TimeoutException =>
          outOfBook.put(game.id)
          none
        }
        .monTry: res =>
          _.fishnet.openingBook(
            variant = game.variant.key.value,
            hit = res.toOption.exists(_.isDefined)
          )

object FishnetOpeningBook:

  trait Depth

  case class Response(moves: List[Move]):

    def randomPonderedMove(turn: Color, level: Int): Option[Move] =
      val sum = moves.map(_.score(turn, level)).sum
      val novelty = 50L * 14 // score of 50 winning games
      val rng = ThreadLocalRandom.nextLong(sum + novelty)
      moves
        .foldLeft((none[Move], 0L)) { case ((found, it), next) =>
          val nextIt = it + next.score(turn, level)
          (found.orElse((nextIt > rng).option(next)), nextIt)
        }
        ._1

  case class Move(uci: Uci, white: Long, draws: Long, black: Long):
    def score(turn: Color, level: Int): Long =
      // interpolate: real frequency at lvl 1, expectation value at lvl 8
      14L * turn.fold(white, black) +
        (15L - level) * draws +
        (16L - 2 * level) * turn.fold(black, white)

  given Reads[Move] = Json.reads
  given Reads[Response] = Json.reads

  private val levelRatings: Map[Int, Seq[Int]] = Map(
    1 -> Seq(400),
    2 -> Seq(1000, 1200),
    3 -> Seq(1400, 1600),
    4 -> Seq(1800, 2000, 2200),
    5 -> Seq(1800, 2000, 2200),
    6 -> Seq(2000, 2200, 2500),
    7 -> Seq(2200, 2500),
    8 -> Seq(2500)
  )

  private val openingSpeeds: Map[Speed, Seq[Speed]] =
    import Speed.*
    Map(
      UltraBullet -> Seq(UltraBullet, Bullet),
      Bullet -> Seq(Bullet, Blitz),
      Blitz -> Seq(Bullet, Blitz, Rapid),
      Rapid -> Seq(Blitz, Rapid, Classical),
      Classical -> Seq(Rapid, Classical, Correspondence),
      Correspondence -> Seq(Rapid, Classical, Correspondence)
    )
