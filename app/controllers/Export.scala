package controllers

import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Game => GameModel, GameRepo, PgnDump }

object Export extends LilaController {

  private def env = Env.game

  private val PngRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 240,
    duration = 1 minute,
    name = "export PNG global",
    key = "export.png.global"
  )

  def png(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      PngRateLimitGlobal("-", msg = s"${HTTPRequest.lastRemoteAddress(ctx.req).value} ${~HTTPRequest.userAgent(ctx.req)}") {
        lila.mon.export.png.game()
        OptionFuResult(GameRepo game id) { game =>
          env.pngExport fromGame game map pngStream
        }
      }
    }
  }

  def puzzlePng(id: Int) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      PngRateLimitGlobal("-", msg = HTTPRequest.lastRemoteAddress(ctx.req).value) {
        lila.mon.export.png.puzzle()
        OptionFuResult(Env.puzzle.api.puzzle find id) { puzzle =>
          env.pngExport(
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

  private def pngStream(stream: play.api.libs.iteratee.Enumerator[Array[Byte]]) =
    Ok.chunked(stream).withHeaders(
      noProxyBufferHeader,
      CONTENT_TYPE -> "image/png",
      CACHE_CONTROL -> "max-age=7200"
    )
}
