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
import lila.core.config.RouteUrl
import lila.core.game.{ ClockHistory, Game, Pov }
import lila.game.GameExt.*
import lila.tree.Analysis
import play.api.mvc.RequestHeader
import lila.common.HTTPRequest.queryStringBoolOpt
import chess.Ply
import chess.format.pgn.Glyph

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
    routeUrl: RouteUrl,
    url: String
)(using Executor):
  private val targetMedianTime = Centis(80)
  private val targetMaxTime = Centis(200)

  def fromPov(
      pov: Pov,
      initialFen: Option[Fen.Full],
      theme: String,
      piece: String,
      analysis: Option[Analysis],
      options: GifExport.Options,
      fromPly: Option[Ply] = None,
      toPly: Option[Ply] = None
  ): Fu[Source[ByteString, ?]] =
    def showPlayer(color: Color) =
      options.players.option:
        Namer.playerTextBlocking(pov.game.players(color), withRating = options.ratings)(using
          lightUserApi.sync
        )
    upstreamResponse(s"pov ${pov.game.id}"):
      lightUserApi.preloadMany(pov.game.userIds) >>
        ws.url(s"$url/game.gif")
          .withMethod("POST")
          .addHttpHeaders("Content-Type" -> "application/json")
          .withBody(
            Json
              .obj(
                "comment" -> s"${routeUrl(routes.Round.watcher(pov.game.id, pov.color))} rendered with https://github.com/lichess-org/lila-gif",
                "orientation" -> pov.color.name,
                "delay" -> targetMedianTime.centis, // default delay for frames
                "frames" -> frames(pov.game, initialFen, analysis, options, fromPly, toPly),
                "theme" -> theme,
                "piece" -> piece
              )
              .add("white", showPlayer(Color.White))
              .add("black", showPlayer(Color.Black))
          )
          .stream()

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
    upstreamResponse(description):
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

  private def glyphsMap(analysis: Option[Analysis]): Map[Ply, Glyph] =
    analysis.fold(Map.empty[Ply, Glyph]): a =>
      a.advices.map(adv => adv.ply -> adv.judgment.glyph).toMap

  private def frames(
      game: Game,
      initialFen: Option[Fen.Full],
      analysis: Option[Analysis],
      options: GifExport.Options,
      fromPly: Option[Ply],
      toPly: Option[Ply]
  ): JsArray =
    val allPositions =
      Position(game.variant, initialFen).playPositions(game.sans).getOrElse(List(game.position))
    val allMoveTimes = scaleMoveTimes(~game.moveTimes).map(some).padTo(allPositions.length, None)
    val from = fromPly.fold(0)(_.value).atLeast(0)
    val to = toPly.fold(allPositions.length - 1)(_.value).atMost(allPositions.length - 1)
    val positions = allPositions.slice(from, to + 1)
    val moveTimes = allMoveTimes.slice(from, to + 1)
    val glyphs = options.glyphs.so(glyphsMap(analysis))
    val clocks = options.clocks.so(game.clockHistory)
    framesRec(
      positions.zip(moveTimes),
      glyphs,
      clocks,
      Ply(from),
      Json.arr()
    )

  @annotation.tailrec
  private def framesRec(
      games: List[(Position, Option[Centis])],
      glyphs: Map[Ply, Glyph],
      clocks: Option[ClockHistory],
      ply: Ply,
      arr: JsArray
  ): JsArray =
    games match
      case Nil => arr
      case (position, scaledMoveTime) :: tail =>
        // longer delay for last frame
        val delay = if tail.isEmpty then Centis(500).some else scaledMoveTime
        val glyph = glyphs.get(ply)
        val whiteClock = clocks.flatMap(_.white.lift((ply.value - 1).atLeast(0) / 2))
        val blackClock = clocks.flatMap(_.black.lift((ply.value - 2).atLeast(0) / 2))
        val f = frame(position, position.history.lastMove, delay, glyph, whiteClock, blackClock)
        framesRec(tail, glyphs, clocks, ply + 1, arr :+ f)

  private def frame(
      position: Position,
      uci: Option[Uci],
      delay: Option[Centis],
      glyph: Option[Glyph],
      whiteClock: Option[Centis],
      blackClock: Option[Centis]
  ) =
    Json
      .obj(
        "fen" -> (Fen.write(position)),
        "lastMove" -> uci.map(UciDump.lastMove(_, position.variant))
      )
      .add("check", position.checkSquare.map(_.key))
      .add("delay", delay.map(_.centis))
      .add("glyph", glyph.map(_.symbol))
      .add(
        "clock",
        (whiteClock.isDefined || blackClock.isDefined).option(
          Json.obj().add("white", whiteClock.map(_.centis)).add("black", blackClock.map(_.centis))
        )
      )
