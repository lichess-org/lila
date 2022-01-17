package controllers

import akka.stream.scaladsl._
import akka.util.ByteString
import chess.Color
import play.api.mvc.Result
import scala.concurrent.duration._

import lila.app._
import lila.game.Pov
import lila.puzzle.Puzzle.Id

final class Export(env: Env) extends LilaController(env) {

  private val ExportImageRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 600,
    duration = 1.minute,
    key = "export.image.global"
  )
  private val ExportGifRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 240,
    duration = 1.minute,
    key = "export.gif.global"
  )

  def gif(id: String, color: String) =
    Open { implicit ctx =>
      OnlyHumansAndFacebookOrTwitter {
        ExportGifRateLimitGlobal("-", msg = ctx.ip.value) {
          OptionFuResult(env.game.gameRepo gameWithInitialFen id) { case (game, initialFen) =>
            val pov = Pov(game, Color.fromName(color) | Color.white)
            env.game.gifExport.fromPov(pov, initialFen) map
              stream("image/gif") map
              gameImageCacheSeconds(game)
          }
        }(rateLimitedFu)
      }
    }

  def legacyGameThumbnail(id: String) =
    Action {
      MovedPermanently(routes.Export.gameThumbnail(id).url)
    }

  def gameThumbnail(id: String) =
    Open { implicit ctx =>
      ExportImageRateLimitGlobal("-", msg = ctx.ip.value) {
        OptionFuResult(env.game.gameRepo game id) { game =>
          env.game.gifExport.gameThumbnail(game) map
            stream("image/gif") map
            gameImageCacheSeconds(game)
        }
      }(rateLimitedFu)
    }

  def puzzleThumbnail(id: String) =
    Open { implicit ctx =>
      ExportImageRateLimitGlobal("-", msg = ctx.ip.value) {
        OptionFuResult(env.puzzle.api.puzzle find Id(id)) { puzzle =>
          env.game.gifExport.thumbnail(
            fen = puzzle.fenAfterInitialMove,
            lastMove = puzzle.line.head.uci.some,
            orientation = puzzle.color
          ) map stream("image/gif") map { res =>
            res.withHeaders(CACHE_CONTROL -> "max-age=1209600")
          }
        }
      }(rateLimitedFu)
    }

  private def gameImageCacheSeconds(game: lila.game.Game)(res: Result): Result = {
    val cacheSeconds =
      if (game.finishedOrAborted) 3600 * 24
      else 10
    res.withHeaders(CACHE_CONTROL -> s"max-age=$cacheSeconds")
  }

  private def stream(contentType: String)(stream: Source[ByteString, _]) =
    Ok.chunked(stream).withHeaders(noProxyBufferHeader) as contentType
}
