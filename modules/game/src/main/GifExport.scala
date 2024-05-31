package lila.game

import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.json._
import play.api.libs.ws.WSClient

import lila.common.Json._
import lila.common.Maths
import lila.common.config.BaseUrl

import shogi.{ Centis, Color, Game => ShogiGame, Replay, Situation }
import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi

final class GifExport(
    ws: WSClient,
    lightUserApi: lila.user.LightUserApi,
    baseUrl: BaseUrl,
    url: String
)(implicit ec: scala.concurrent.ExecutionContext) {
  private val targetMedianTime = Centis(100)
  private val targetMaxTime    = Centis(250)

  def fromPov(pov: Pov): Fu[Option[Source[ByteString, _]]] = {
    if (Game.gifVariants.contains(pov.game.variant))
      lightUserApi preloadMany pov.game.userIds flatMap { _ =>
        ws.url(s"${url}/game.gif")
          .withMethod("POST")
          .addHttpHeaders("Content-Type" -> "application/json")
          .withBody(
            Json.obj(
              "black" -> Namer.playerTextBlocking(pov.game.sentePlayer, withRating = true)(lightUserApi.sync),
              "white" -> Namer.playerTextBlocking(pov.game.gotePlayer, withRating = true)(lightUserApi.sync),
              "comment" -> s"${baseUrl.value}/${pov.game.id} rendered with https://github.com/WandererXII/lishogi-gif",
              "orientation" -> pov.color.engName,
              "delay"       -> targetMedianTime.centis, // default delay for frames
              "frames"      -> frames(pov.game)
            )
          )
          .stream() flatMap {
          case res if res.status != 200 =>
            logger.warn(s"GifExport pov ${pov.game.id} ${res.status}")
            fufail(res.statusText)
          case res => fuccess(res.bodyAsSource.some)
        }
      }
    else fuccess(none)
  }

  def gameThumbnail(game: Game): Fu[Option[Source[ByteString, _]]] = {
    if (Game.gifVariants.contains(game.variant)) {
      val query = List(
        "sfen"        -> game.shogi.toSfen.value,
        "black"       -> Namer.playerTextBlocking(game.sentePlayer, withRating = true)(lightUserApi.sync),
        "white"       -> Namer.playerTextBlocking(game.gotePlayer, withRating = true)(lightUserApi.sync),
        "orientation" -> game.firstColor.engName
      ) ::: List(
        game.lastUsiStr.map { "lastMove" -> _ },
        game.situation.checkSquares.headOption.map { "check" -> _.key }
      ).flatten

      lightUserApi preloadMany game.userIds flatMap { _ =>
        ws.url(s"${url}/image.gif")
          .withMethod("GET")
          .withQueryStringParameters(query: _*)
          .stream() flatMap {
          case res if res.status != 200 =>
            logger.warn(s"GifExport gameThumbnail ${game.id} ${res.status}")
            fufail(res.statusText)
          case res => fuccess(res.bodyAsSource.some)
        }
      }
    } else fuccess(none)
  }

  def thumbnail(sfen: Sfen, lastUsi: Option[String], orientation: Color): Fu[Source[ByteString, _]] = {
    val query = List(
      "sfen"        -> sfen.value,
      "orientation" -> orientation.engName
    ) ::: List(
      lastUsi.map { "lastMove" -> _ }
    ).flatten

    ws.url(s"${url}/image.gif")
      .withMethod("GET")
      .withQueryStringParameters(query: _*)
      .stream() flatMap {
      case res if res.status != 200 =>
        logger.warn(s"GifExport thumbnail ${sfen} ${res.status}")
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

  private def frames(game: Game) = {
    Replay.gamesWhileValid(
      game.usis,
      game.initialSfen,
      game.variant
    ) match {
      case (games, _) =>
        val steps = (games.head, None) :: games.tail.zip(game.usis.map(_.some))
        framesRec(
          steps.zip(scaleMoveTimes(~game.moveTimes).map(_.some).padTo(steps.length, None)),
          Json.arr()
        )
    }
  }

  @annotation.tailrec
  private def framesRec(games: List[((ShogiGame, Option[Usi]), Option[Centis])], arr: JsArray): JsArray =
    games match {
      case ((game, usi), scaledMoveTime) :: tail =>
        // longer delay for last frame
        val delay = if (tail.isEmpty) Centis(500).some else scaledMoveTime
        framesRec(tail, arr :+ frame(game.situation, usi, delay))
      case _ => arr
    }

  private def frame(situation: Situation, usi: Option[Usi], delay: Option[Centis]) =
    Json
      .obj(
        "sfen"     -> situation.toSfen,
        "lastMove" -> usi.map(_.usi)
      )
      .add("check", situation.checkSquares.headOption.map(_.key))
      .add("delay", delay.map(_.centis))
}
