package lila.fishnet

import chess.format.Forsyth
import chess.format.Uci
import chess.{ Color, Speed }
import com.softwaremill.tagging._
import play.api.libs.json._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import lila.common.Json._
import lila.common.ThreadLocalRandom
import lila.game.Game
import lila.memo.SettingStore
import scala.util.{ Failure, Success }

final private class FishnetOpeningBook(
    ws: StandaloneWSClient,
    depth: SettingStore[Int] @@ FishnetOpeningBook.Depth,
    config: FishnetConfig
)(implicit ec: ExecutionContext) {

  import FishnetOpeningBook._

  private val outOfBook = new lila.memo.ExpireSetMemo(10 minutes)

  def apply(game: Game, level: Int): Fu[Option[Uci]] =
    (game.turns < depth.get() && !outOfBook.get(game.id)) ?? {
      ws.url(s"${config.explorerEndpoint}/lichess")
        .withQueryStringParameters(
          "variant"     -> game.variant.key,
          "fen"         -> Forsyth.>>(game.chess).value,
          "topGames"    -> "0",
          "recentGames" -> "0",
          "ratings"     -> (~levelRatings.get(level)).mkString(","),
          "speeds"      -> (~openingSpeeds.get(game.speed)).map(_.key).mkString(",")
        )
        .get()
        .map {
          case res if res.status != 200 =>
            logger.warn(s"opening book ${game.id} ${level} ${res.status} ${res.body}")
            none
          case res =>
            for {
              data <- res.body[JsValue].validate[Response](responseReader).asOpt
              _ = if (data.moves.isEmpty) outOfBook.put(game.id)
              move <- data randomPonderedMove (game.turnColor, level)
            } yield move.uci
        }
        .monTry { res =>
          _.fishnet
            .openingBook(
              level = level,
              variant = game.variant.key,
              ply = game.turns,
              hit = res.toOption.exists(_.isDefined),
              success = res.isSuccess
            )
        }
    }
}

object FishnetOpeningBook {

  trait Depth

  case class Response(moves: List[Move]) {

    def randomPonderedMove(turn: Color, level: Int): Option[Move] = {
      val sum     = moves.map(_.score(turn, level)).sum
      val novelty = 5L * 14 // score of 5 winning games
      val rng     = ThreadLocalRandom.nextLong(sum + novelty)
      moves
        .foldLeft((none[Move], 0L)) { case ((found, it), next) =>
          val nextIt = it + next.score(turn, level)
          (found orElse (nextIt > rng).option(next), nextIt)
        }
        ._1
    }
  }

  case class Move(uci: Uci, white: Long, draws: Long, black: Long) {
    def score(turn: Color, level: Int): Long =
      // interpolate: real frequency at lvl 1, expectation value at lvl 8
      14L * turn.fold(white, black) +
        (15L - level) * draws +
        (16L - 2 * level) * turn.fold(black, white)
  }

  implicit val moveReader     = Json.reads[Move]
  implicit val responseReader = Json.reads[Response]

  private val levelRatings: Map[Int, Seq[Int]] = Map(
    1 -> Seq(1600),
    2 -> Seq(1600, 1800),
    3 -> Seq(1800, 2000),
    4 -> Seq(1800, 2000, 2200),
    5 -> Seq(1800, 2000, 2200),
    6 -> Seq(2000, 2200, 2500),
    7 -> Seq(2200, 2500),
    8 -> Seq(2500)
  )

  private val openingSpeeds: Map[Speed, Seq[Speed]] = {
    import Speed._
    Map(
      UltraBullet    -> Seq(UltraBullet, Bullet),
      Bullet         -> Seq(Bullet, Blitz),
      Blitz          -> Seq(Bullet, Blitz, Rapid),
      Rapid          -> Seq(Blitz, Rapid, Classical),
      Classical      -> Seq(Rapid, Classical, Correspondence),
      Correspondence -> Seq(Rapid, Classical, Correspondence)
    )
  }
}
