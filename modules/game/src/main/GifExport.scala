package lila.game

import akka.stream.scaladsl.*
import akka.util.ByteString
import chess.format.{ Fen, Uci, UciDump }
import chess.{ Position, Centis, Color }
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse }
import scalalib.Maths

import lila.common.Json.given
import lila.core.config.BaseUrl
import lila.core.game.{ Game, Pov }
import lila.game.GameExt.*
import lila.tree.Analysis
import play.api.mvc.RequestHeader
import lila.common.HTTPRequest.queryStringBoolOpt

object GifExport:
  case class UpstreamStatus(code: Int) extends lila.core.lilaism.LilaException:
    val message = s"gif service status: $code"

  case class Options(
      players: Boolean = true,
      ratings: Boolean = true,
      clocks: Boolean = true,
      glyphs: Boolean = true
  ):
    def makeSense = copy(ratings = players && ratings)
  object Options:
    val default = Options()
    def fromReq(using RequestHeader): Options =
      Options(
        players = queryStringBoolOpt("players") | default.players,
        ratings = queryStringBoolOpt("ratings") | default.ratings,
        clocks = queryStringBoolOpt("clocks") | default.clocks,
        glyphs = queryStringBoolOpt("glyphs") | default.glyphs
      ).makeSense

final class GifExport(
    ws: StandaloneWSClient,
    lightUserApi: lila.core.user.LightUserApi,
    baseUrl: BaseUrl,
    url: String
)(using Executor):
  private val targetMedianTime = Centis(80)
  private val targetMaxTime = Centis(200)

  def fromPov(
      pov: Pov,
      initialFen: Option[Fen.Full],
      theme: String,
      piece: String,
      analysis: Option[Analysis] = None,
      options: GifExport.Options = GifExport.Options()
  ): Fu[Source[ByteString, ?]] =
    (lightUserApi.preloadMany(pov.game.userIds) >>
      ws.url(s"$url/game.gif")
        .withMethod("POST")
        .addHttpHeaders("Content-Type" -> "application/json")
        .withBody(
          Json
            .obj(
              "comment" -> s"${baseUrl.value}/${pov.game.id} rendered with https://github.com/lichess-org/lila-gif",
              "orientation" -> pov.color.name,
              "delay" -> targetMedianTime.centis, // default delay for frames
              "frames" -> frames(pov.game, initialFen, analysis, options),
              "theme" -> theme,
              "piece" -> piece
            )
            .add(
              "white",
              options.players.option(
                Namer.playerTextBlocking(pov.game.whitePlayer, withRating = options.ratings)(using
                  lightUserApi.sync
                )
              )
            )
            .add(
              "black",
              options.players.option(
                Namer.playerTextBlocking(pov.game.blackPlayer, withRating = options.ratings)(using
                  lightUserApi.sync
                )
              )
            )
            .add("clocks", clocksJson(pov.game, options))
        )
        .stream()).pipe(upstreamResponse(s"pov ${pov.game.id}"))

  def gameThumbnail(game: Game, theme: String, piece: String): Fu[Source[ByteString, ?]] =
    lightUserApi.preloadMany(game.userIds) >>
      thumbnail(
        position = game.chess.position,
        white = Namer.playerTextBlocking(game.whitePlayer, withRating = true)(using lightUserApi.sync).some,
        black = Namer.playerTextBlocking(game.blackPlayer, withRating = true)(using lightUserApi.sync).some,
        orientation = game.naturalOrientation,
        lastMove = game.history.lastMove,
        theme = theme,
        piece = piece,
        description = s"gameThumbnail ${game.id}"
      )

  def thumbnail(
      position: Position,
      white: Option[String] = None,
      black: Option[String] = None,
      orientation: Color,
      lastMove: Option[Uci],
      theme: String,
      piece: String,
      description: String
  ): Fu[Source[ByteString, ?]] =
    ws.url(s"$url/image.gif")
      .withMethod("GET")
      .withQueryStringParameters(
        List(
          "fen" -> Fen.write(position).value,
          "orientation" -> orientation.name,
          "theme" -> theme,
          "piece" -> piece
        ) ::: List(
          white.map { "white" -> _ },
          black.map { "black" -> _ },
          lastMove.map { lm => "lastMove" -> UciDump.lastMove(lm, position.variant) },
          position.checkSquare.map { "check" -> _.key }
        ).flatten*
      )
      .stream()
      .pipe(upstreamResponse(description))

  private def upstreamResponse(
      description: String
  )(res: Fu[StandaloneWSResponse]): Fu[Source[ByteString, ?]] =
    res.flatMap:
      case res if res.status != 200 =>
        logger.warn(s"GifExport $description ${res.status}")
        fufail(GifExport.UpstreamStatus(res.status))
      case res => fuccess(res.bodyAsSource)

  private def scaleMoveTimes(moveTimes: Vector[Centis]): Vector[Centis] =
    // goal for bullet: close to real-time
    // goal for classical: speed up to reach target median, avoid extremely
    // fast moves, unless they were actually played instantly
    Maths.median(moveTimes.map(_.centis)).map(Centis.ofDouble(_)).filter(_ >= targetMedianTime) match
      case Some(median) =>
        val scale = targetMedianTime.centis.toFloat / median.centis.atLeast(1).toFloat
        moveTimes.map { t =>
          if t * 2 < median then t.atMost(targetMedianTime *~ 0.5)
          else (t *~ scale).atLeast(targetMedianTime *~ 0.5).atMost(targetMaxTime)
        }
      case None => moveTimes.map(_.atMost(targetMaxTime))

  private def clocksJson(game: Game, options: GifExport.Options): Option[JsObject] =
    options.clocks.so:
      game.clockHistory.map: history =>
        Json.obj(
          "white" -> history.white.map(_.centis),
          "black" -> history.black.map(_.centis)
        )

  private def glyphsMap(analysis: Option[Analysis], options: GifExport.Options): Map[Int, String] =
    options.glyphs.so:
      analysis.fold(Map.empty[Int, String]): a =>
        a.advices.map(adv => adv.ply.value -> adv.judgment.glyph.symbol).toMap

  private def frames(
      game: Game,
      initialFen: Option[Fen.Full],
      analysis: Option[Analysis],
      options: GifExport.Options
  ): JsArray =
    val positions = Position(game.variant, initialFen).playPositions(game.sans).getOrElse(List(game.position))
    val glyphs = glyphsMap(analysis, options)
    framesRec(
      positions.zip(scaleMoveTimes(~game.moveTimes).map(some).padTo(positions.length, None)),
      glyphs,
      0,
      Json.arr()
    )

  @annotation.tailrec
  private def framesRec(
      games: List[(Position, Option[Centis])],
      glyphs: Map[Int, String],
      ply: Int,
      arr: JsArray
  ): JsArray =
    games match
      case Nil => arr
      case (position, scaledMoveTime) :: tail =>
        // longer delay for last frame
        val delay = if tail.isEmpty then Centis(500).some else scaledMoveTime
        val glyph = glyphs.get(ply)
        framesRec(tail, glyphs, ply + 1, arr :+ frame(position, position.history.lastMove, delay, glyph))

  private def frame(position: Position, uci: Option[Uci], delay: Option[Centis], glyph: Option[String]) =
    Json
      .obj(
        "fen" -> (Fen.write(position)),
        "lastMove" -> uci.map(UciDump.lastMove(_, position.variant))
      )
      .add("check", position.checkSquare.map(_.key))
      .add("delay", delay.map(_.centis))
      .add("glyph", glyph)
