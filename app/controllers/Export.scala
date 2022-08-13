package controllers

import akka.stream.scaladsl._
import akka.util.ByteString
import chess.Color
import chess.format.{ FEN, Forsyth, Uci }
import chess.variant.{ Standard, Variant }
import play.api.mvc.{ RequestHeader, Result }
import scala.concurrent.duration._

import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import lila.game.Pov
import lila.puzzle.Puzzle.Id

final class Export(env: Env) extends LilaController(env) {

  private val ExportImageRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 600,
    duration = 1.minute,
    key = "export.image.global"
  )
  private val ExportImageRateLimitByIp = new lila.memo.RateLimit[IpAddress](
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

  def gif(id: String, color: String) =
    exportImageOf(env.game.gameRepo gameWithInitialFen id) { g =>
      env.game.gifExport.fromPov(Pov(g.game, Color.fromName(color) | Color.white), g.fen) map
        stream(cacheSeconds = if (g.game.finishedOrAborted) 3600 * 24 else 10)
    }

  def legacyGameThumbnail(id: String) =
    Action {
      MovedPermanently(routes.Export.gameThumbnail(id).url)
    }

  def gameThumbnail(id: String) = exportImageOf(env.game.gameRepo game id) { game =>
    env.game.gifExport.gameThumbnail(game) map
      stream(cacheSeconds = if (game.finishedOrAborted) 3600 * 24 else 10)
  }

  def puzzleThumbnail(id: String) =
    exportImageOf(env.puzzle.api.puzzle find Id(id)) { puzzle =>
      env.game.gifExport.thumbnail(
        fen = puzzle.fenAfterInitialMove,
        lastMove = puzzle.line.head.uci.some,
        orientation = puzzle.color,
        variant = Standard
      ) map stream()
    }

  def fenThumbnail(fen: String, color: String, lastMove: Option[String], variant: Option[String]) =
    exportImageOf(fuccess(Forsyth << FEN.clean(fen))) { _ =>
      env.game.gifExport.thumbnail(
        fen = FEN clean fen,
        lastMove = lastMove flatMap (Uci.Move(_).map(_.uci)),
        orientation = Color.fromName(color) | Color.White,
        variant = Variant(variant.getOrElse("standard")) | Standard
      ) map stream()
    }

  private def stream(contentType: String = "image/gif", cacheSeconds: Int = 1209600)(
      stream: Source[ByteString, _]
  ) =
    Ok.chunked(stream)
      .withHeaders(noProxyBufferHeader)
      .withHeaders(CACHE_CONTROL -> s"max-age=$cacheSeconds")
      .as(contentType)
}
