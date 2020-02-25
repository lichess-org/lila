package controllers

import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest

final class Export(env: Env) extends LilaController(env) {

  private val ExportRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 240,
    duration = 1 minute,
    name = "export PNG global",
    key = "export.png.global"
  )

  def png(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      ExportRateLimitGlobal(
        "-",
        msg = s"${HTTPRequest.lastRemoteAddress(ctx.req).value} ${~HTTPRequest.userAgent(ctx.req)}"
      ) {
        lila.mon.export.png.game.increment()
        OptionFuResult(env.game.gameRepo game id) { game =>
          env.game.pngExport fromGame game map stream("image/png")
        }
      }
    }
  }

  def gif(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      ExportRateLimitGlobal("-", msg = HTTPRequest.lastRemoteAddress(ctx.req).value) {
        OptionFuResult(env.game.gameRepo gameWithInitialFen id) {
          case (game, initialFen) =>
            env.game.gifExport.fromGame(game, initialFen) map stream("image/gif")
        }
      }
    }
  }

  def puzzlePng(id: Int) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      ExportRateLimitGlobal("-", msg = HTTPRequest.lastRemoteAddress(ctx.req).value) {
        lila.mon.export.png.puzzle.increment()
        OptionFuResult(env.puzzle.api.puzzle find id) { puzzle =>
          env.game.pngExport(
            fen = chess.format.FEN(puzzle.fenAfterInitialMove | puzzle.fen),
            lastMove = puzzle.initialMove.uci.some,
            check = none,
            orientation = puzzle.color.some,
            logHint = s"puzzle $id"
          ) map stream("image/png")
        }
      }
    }
  }

  private def stream(contentType: String)(stream: Source[ByteString, _]) =
    Ok.chunked(stream)
      .withHeaders(
        noProxyBufferHeader,
        CACHE_CONTROL -> "max-age=7200"
      ) as contentType
}
