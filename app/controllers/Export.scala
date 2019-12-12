package controllers

import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest

final class Export(env: Env) extends LilaController(env) {

  private val PngRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 240,
    duration = 1 minute,
    name = "export PNG global",
    key = "export.png.global"
  )

  def png(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      PngRateLimitGlobal("-", msg = s"${HTTPRequest.lastRemoteAddress(ctx.req).value} ${~HTTPRequest.userAgent(ctx.req)}") {
        lila.mon.export.png.game.increment()
        OptionFuResult(env.game.gameRepo game id) { game =>
          env.game.pngExport fromGame game map pngStream
        }
      }
    }
  }

  def puzzlePng(id: Int) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      PngRateLimitGlobal("-", msg = HTTPRequest.lastRemoteAddress(ctx.req).value) {
        lila.mon.export.png.puzzle.increment()
        OptionFuResult(env.puzzle.api.puzzle find id) { puzzle =>
          env.game.pngExport(
            fen = chess.format.FEN(puzzle.fenAfterInitialMove | puzzle.fen),
            lastMove = puzzle.initialMove.uci.some,
            check = none,
            orientation = puzzle.color.some,
            logHint = s"puzzle $id"
          ) map pngStream
        }
      }
    }
  }

  private def pngStream(stream: Source[ByteString, _]) =
    Ok.chunked(stream).withHeaders(
      noProxyBufferHeader,
      CACHE_CONTROL -> "max-age=7200"
    ) as "image/png"
}
