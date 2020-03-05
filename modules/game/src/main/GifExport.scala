package lila.game

import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.json._
import play.api.libs.ws.WSClient

import lila.common.Maths
import lila.common.config.BaseUrl

import chess.{ Centis, Replay, Situation, Game => ChessGame }
import chess.format.{ FEN, Forsyth, Uci }

final class GifExport(
    ws: WSClient,
    lightUserApi: lila.user.LightUserApi,
    baseUrl: BaseUrl,
    url: String
)(implicit ec: scala.concurrent.ExecutionContext) {
  private val targetMedianTime = 80.0

  def fromPov(pov: Pov, initialFen: Option[FEN]): Fu[Source[ByteString, _]] =
    lightUserApi preloadMany pov.game.userIds flatMap { _ =>
      ws.url(url)
        .withMethod("POST")
        .addHttpHeaders("Content-Type" -> "application/json")
        .withBody(
          Json.obj(
            "white"       -> Namer.playerTextBlocking(pov.game.whitePlayer, withRating = true)(lightUserApi.sync),
            "black"       -> Namer.playerTextBlocking(pov.game.blackPlayer, withRating = true)(lightUserApi.sync),
            "comment"     -> s"${baseUrl.value}/${pov.game.id} rendered with https://github.com/niklasf/lila-gif",
            "orientation" -> pov.color.name,
            "delay"       -> targetMedianTime.toInt, // default delay for frames, centis
            "frames"      -> frames(pov.game, initialFen)
          )
        )
        .stream() flatMap {
        case res if res.status != 200 =>
          logger.warn(s"GifExport game ${pov.game.id} ${res.status}")
          fufail(res.statusText)
        case res => fuccess(res.bodyAsSource)
      }
    }

  private def scaleMoveTimes(moveTimes: Vector[Centis]): Vector[Centis] = {
    val targetMax = Centis(200)
    Maths.median(moveTimes.map(_.centis)).filter(_ >= targetMedianTime) match {
      case Some(median) => moveTimes.map(_ *~ (targetMedianTime / median.atLeast(1)) atMost targetMax)
      case None         => moveTimes.map(_ atMost targetMax)
    }
  }

  private def frames(game: Game, initialFen: Option[FEN]) = {
    Replay.gameMoveWhileValid(
      game.pgnMoves,
      initialFen.map(_.value) | game.variant.initialFen,
      game.variant
    ) match {
      case (init, games, _) =>
        val steps = (init, None) :: (games map {
          case (g, Uci.WithSan(uci, _)) => (g, uci.some)
        })
        framesRec(steps.zip(game.moveTimes match {
          case Some(moveTimes) => scaleMoveTimes(moveTimes).map(_.some)
          case None            => LazyList.continually(None)
        }), Json.arr())
    }
  }

  @annotation.tailrec
  private def framesRec(games: List[((ChessGame, Option[Uci]), Option[Centis])], arr: JsArray): JsArray =
    games match {
      case Nil =>
        arr
      case ((game, uci), scaledMoveTime) :: tail =>
        // longer delay for last frame
        val delay = if (tail.isEmpty) Centis(500).some else scaledMoveTime
        framesRec(tail, arr :+ frame(game.situation, uci, delay))
    }

  private def frame(situation: Situation, uci: Option[Uci], delay: Option[Centis]) =
    Json
      .obj(
        "fen"      -> (Forsyth >> situation),
        "lastMove" -> uci.map(_.uci)
      )
      .add("check", situation.checkSquare.map(_.key))
      .add("delay", delay.map(_.centis))
}
