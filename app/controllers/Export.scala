package controllers

import akka.stream.scaladsl.*
import akka.util.ByteString

import chess.format.{ Fen, Uci }
import chess.variant.Variant
import play.api.mvc.Result

import lila.app.*
import lila.core.net.IpAddress
import lila.pref.{ PieceSet, Theme }
import lila.core.id.PuzzleId

final class Export(env: Env) extends LilaController(env):

  private def exportImageOf[A](fetch: Fu[Option[A]])(convert: A => Fu[Result]) = Anon:
    Found(fetch): res =>
      limit.exportImage(((), req.ipAddress), rateLimited)(convert(res))

  def gif(id: GameId, color: Color, theme: Option[String], piece: Option[String]) =
    exportImageOf(env.game.gameRepo.gameWithInitialFen(id)) { g =>
      env.game.gifExport
        .fromPov(Pov(g.game, color), g.fen, Theme(theme).name, PieceSet.get(piece).name)
        .pipe(stream(cacheSeconds = if g.game.finishedOrAborted then 3600 * 24 else 10))
    }

  def legacyGameThumbnail(id: GameId, theme: Option[String], piece: Option[String]) = Anon:
    MovedPermanently(routes.Export.gameThumbnail(id, theme, piece).url)

  def gameThumbnail(id: GameId, theme: Option[String], piece: Option[String]) =
    exportImageOf(env.game.gameRepo.game(id)) { game =>
      env.game.gifExport
        .gameThumbnail(game, Theme(theme).name, PieceSet.get(piece).name)
        .pipe(stream(cacheSeconds = if game.finishedOrAborted then 3600 * 24 else 10))
    }

  def puzzleThumbnail(id: PuzzleId, theme: Option[String], piece: Option[String]) =
    exportImageOf(env.puzzle.api.puzzle.find(id)) { puzzle =>
      env.game.gifExport
        .thumbnail(
          situation = puzzle.situationAfterInitialMove.err(s"invalid puzzle ${puzzle.id}"),
          lastMove = puzzle.line.head.some,
          orientation = puzzle.color,
          theme = Theme(theme).name,
          piece = PieceSet.get(piece).name,
          description = s"puzzleThumbnail ${puzzle.id}"
        )
        .pipe(stream())
    }

  def fenThumbnail(
      fen: String,
      color: Option[Color],
      lastMove: Option[Uci],
      variant: Option[Variant.LilaKey],
      theme: Option[String],
      piece: Option[String]
  ) =
    exportImageOf(fuccess(Fen.read(Variant.orDefault(variant), Fen.Full.clean(fen)))) { situation =>
      env.game.gifExport
        .thumbnail(
          situation = situation,
          lastMove = lastMove,
          orientation = color | Color.white,
          theme = Theme(theme).name,
          piece = PieceSet.get(piece).name,
          description = s"fenThumbnail $fen"
        )
        .pipe(stream())
    }

  private def stream(contentType: String = "image/gif", cacheSeconds: Int = 1209600)(
      upstream: Fu[Source[ByteString, ?]]
  ): Fu[Result] = upstream
    .map: stream =>
      Ok.chunked(stream)
        .withHeaders(CACHE_CONTROL -> s"max-age=$cacheSeconds")
        .as(contentType)
        .pipe(noProxyBuffer)
    .recover { case lila.game.GifExport.UpstreamStatus(code) => Status(code) }
