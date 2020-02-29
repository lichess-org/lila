package controllers

import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent.duration._
import play.api.mvc.Result

import lila.app._
import lila.common.HTTPRequest

final class Export(env: Env) extends LilaController(env) {

  private val ExportRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 240,
    duration = 1 minute,
    name = "export image global",
    key = "export.image.global"
  )

  def png(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      ExportRateLimitGlobal(
        "-",
        msg = s"${HTTPRequest.lastRemoteAddress(ctx.req).value} ${~HTTPRequest.userAgent(ctx.req)}"
      ) {
        lila.mon.export.png.game.increment()
        OptionFuResult(env.game.gameRepo game id) { game =>
          env.game.pngExport fromGame game map
            stream("image/png") map
            gameImageCacheSeconds(game)
        }
      }
    }
  }

  def gif(id: String, color: String) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      ExportRateLimitGlobal("-", msg = HTTPRequest.lastRemoteAddress(ctx.req).value) {
        OptionFuResult(env.game.gameRepo povWithInitialFen(id, color)) {
          case (pov, initialFen) =>
            env.game.gifExport.fromPov(pov, initialFen) map
              stream("image/gif") map
              gameImageCacheSeconds(pov.game)
        }
      }
    }
  }

  private def gameImageCacheSeconds(game: lila.game.Game)(res: Result): Result = {
    val cacheSeconds =
      if (game.finishedOrAborted) 3600 * 24
      else 10
    res.withHeaders(CACHE_CONTROL -> s"max-age=$cacheSeconds")
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
