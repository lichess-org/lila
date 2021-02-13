package lila.game

import akka.stream.scaladsl._
import akka.util.ByteString
import chess.format.{ FEN, Forsyth, Uci }
import chess.{ Centis, Color, Replay, Situation, Game => ChessGame }
import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.BaseUrl
import lila.common.Json._
import lila.common.Maths

final class GifExport(
    ws: StandaloneWSClient,
    lightUserApi: lila.user.LightUserApi,
    baseUrl: BaseUrl,
    url: String
)(implicit ec: scala.concurrent.ExecutionContext) {
  private val targetMedianTime = Centis(80)
  private val targetMaxTime    = Centis(200)

  def fromPov(pov: Pov, initialFen: Option[FEN]): Fu[Source[ByteString, _]] =
    lightUserApi preloadMany pov.game.userIds flatMap { _ =>
      ws.url(s"$url/game.gif")
        .withMethod("POST")
        .addHttpHeaders("Content-Type" -> "application/json")
        .withBody(
          Json.obj(
            "white"       -> Namer.playerTextBlocking(pov.game.whitePlayer, withRating = true)(lightUserApi.sync),
            "black"       -> Namer.playerTextBlocking(pov.game.blackPlayer, withRating = true)(lightUserApi.sync),
            "comment"     -> s"${baseUrl.value}/${pov.game.id} rendered with https://github.com/niklasf/lila-gif",
            "orientation" -> pov.color.name,
            "delay"       -> targetMedianTime.centis, // default delay for frames
            "frames"      -> frames(pov.game, initialFen)
          )
        )
        .stream() flatMap {
        case res if res.status != 200 =>
          logger.warn(s"GifExport pov ${pov.game.id} ${res.status}")
          fufail(res.statusText)
        case res => fuccess(res.bodyAsSource)
      }
    }

  def gameThumbnail(game: Game): Fu[Source[ByteString, _]] = {
    val query = List(
      "fen"         -> (Forsyth >> game.chess).value,
      "white"       -> Namer.playerTextBlocking(game.whitePlayer, withRating = true)(lightUserApi.sync),
      "black"       -> Namer.playerTextBlocking(game.blackPlayer, withRating = true)(lightUserApi.sync),
      "orientation" -> game.naturalOrientation.name
    ) ::: List(
      game.lastMoveKeys.map { "lastMove" -> _ },
      game.situation.checkSquare.map { "check" -> _.key }
    ).flatten

    lightUserApi preloadMany game.userIds flatMap { _ =>
      ws.url(s"$url/image.gif")
        .withMethod("GET")
        .withQueryStringParameters(query: _*)
        .stream() flatMap {
        case res if res.status != 200 =>
          logger.warn(s"GifExport gameThumbnail ${game.id} ${res.status}")
          fufail(res.statusText)
        case res => fuccess(res.bodyAsSource)
      }
    }
  }

  def thumbnail(fen: FEN, lastMove: Option[String], orientation: Color): Fu[Source[ByteString, _]] = {
    val query = List(
      "fen"         -> fen.value,
      "orientation" -> orientation.name
    ) ::: List(
      lastMove.map { "lastMove" -> _ }
    ).flatten

    ws.url(s"$url/image.gif")
      .withMethod("GET")
      .withQueryStringParameters(query: _*)
      .stream() flatMap {
      case res if res.status != 200 =>
        logger.warn(s"GifExport thumbnail $fen ${res.status}")
        fufail(res.statusText)
      case res => fuccess(res.bodyAsSource)
    }
  }

  private def scaleMoveTimes(moveTimes: Vector[Centis]): Vector[Centis] = {
    // goal for bullet: close to real-time
    // goal for classical: speed up to reach target median, avoid extremely
    // fast moves, unless they were actually played instantly
    Maths.median(moveTimes.map(_.centis)).map(Centis.apply).filter(_ >= targetMedianTime) match {
      case Some(median) =>
        val scale = targetMedianTime.centis.toDouble / median.centis.atLeast(1).toDouble
        moveTimes.map { t =>
          if (t * 2 < median) t atMost (targetMedianTime *~ 0.5)
          else t *~ scale atLeast (targetMedianTime *~ 0.5) atMost targetMaxTime
        }
      case None => moveTimes.map(_ atMost targetMaxTime)
    }
  }

  private def frames(game: Game, initialFen: Option[FEN]) = {
    Replay.gameMoveWhileValid(
      game.pgnMoves,
      initialFen | game.variant.initialFen,
      game.variant
    ) match {
      case (init, games, _) =>
        val steps = (init, None) :: (games map { case (g, Uci.WithSan(uci, _)) =>
          (g, uci.some)
        })
        framesRec(
          steps.zip(scaleMoveTimes(~game.moveTimes).map(_.some).padTo(steps.length, None)),
          Json.arr()
        )
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
