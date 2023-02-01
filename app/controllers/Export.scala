package controllers

import akka.stream.scaladsl.*
import akka.util.ByteString
import chess.Color
import chess.format.{ Fen, Uci }
import chess.variant.{ Standard, Variant }
import play.api.mvc.{ RequestHeader, Result }
import scala.util.chaining.*

import lila.app.{ given, * }
import lila.common.{ HTTPRequest, IpAddress }
import lila.game.Pov
import lila.pref.{ PieceSet, Theme }

final class Export(env: Env) extends LilaController(env):

  private val ExportImageRateLimitGlobal = lila.memo.RateLimit[String](
    credits = 600,
    duration = 1.minute,
    key = "export.image.global"
  )
  private val ExportImageRateLimitByIp = lila.memo.RateLimit[IpAddress](
    credits = 15,
    duration = 1.minute,
    key = "export.image.ip"
  )

  private def exportImageOf[A](fetch: Fu[Option[A]])(convert: A => Fu[Result]) =
    Action.async { implicit req =>
      fetch flatMap {
        _.fold(notFoundJson()) { res =>
          ExportImageRateLimitByIp(HTTPRequest ipAddress req) {
            ExportImageRateLimitGlobal("-") {
              convert(res)
            }(rateLimitedFu)
          }(rateLimitedFu)
        }
      }
    }

  def gif(id: GameId, color: String, theme: Option[String], piece: Option[String]) =
    exportImageOf(env.game.gameRepo gameWithInitialFen id) { g =>
      env.game.gifExport.fromPov(
        Pov(g.game, Color.fromName(color) | Color.white),
        g.fen,
        Theme(theme).name,
        PieceSet.get(piece).name
      ) pipe stream(cacheSeconds = if (g.game.finishedOrAborted) 3600 * 24 else 10)
    }

  def legacyGameThumbnail(id: GameId, theme: Option[String], piece: Option[String]) =
    Action {
      MovedPermanently(routes.Export.gameThumbnail(id, theme, piece).url)
    }

  def gameThumbnail(id: GameId, theme: Option[String], piece: Option[String]) =
    exportImageOf(env.game.gameRepo game id) { game =>
      env.game.gifExport.gameThumbnail(game, Theme(theme).name, PieceSet.get(piece).name) pipe
        stream(cacheSeconds = if (game.finishedOrAborted) 3600 * 24 else 10)
    }

  def puzzleThumbnail(id: PuzzleId, theme: Option[String], piece: Option[String]) =
    exportImageOf(env.puzzle.api.puzzle find id) { puzzle =>
      env.game.gifExport.thumbnail(
        fen = puzzle.fenAfterInitialMove,
        lastMove = puzzle.line.head.some,
        orientation = puzzle.color,
        variant = Standard,
        Theme(theme).name,
        PieceSet.get(piece).name
      ) pipe stream()
    }

  def fenThumbnail(
      fen: String,
      color: String,
      lastMove: Option[Uci],
      variant: Option[Variant.LilaKey],
      theme: Option[String],
      piece: Option[String]
  ) =
    exportImageOf(fuccess(Fen read Fen.Epd.clean(fen))) { _ =>
      env.game.gifExport.thumbnail(
        fen = Fen.Epd.clean(fen),
        lastMove = lastMove,
        orientation = Color.fromName(color) | Color.White,
        variant = Variant.orDefault(variant),
        Theme(theme).name,
        PieceSet.get(piece).name
      ) pipe stream()
    }

  private def stream(contentType: String = "image/gif", cacheSeconds: Int = 1209600)(
      upstream: Fu[Source[ByteString, ?]]
  ): Fu[Result] = upstream
    .map { stream =>
      Ok.chunked(stream)
        .withHeaders(noProxyBufferHeader)
        .withHeaders(CACHE_CONTROL -> s"max-age=$cacheSeconds")
        .as(contentType)
    }
    .recover { case lila.game.GifExport.UpstreamStatus(code) => Status(code) }
