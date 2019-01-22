package controllers

import scala.concurrent.duration._

import lidraughts.app._
import lidraughts.common.HTTPRequest
import lidraughts.pref.Pref.puzzleVariants
import lidraughts.game.{ Game => GameModel, GameRepo, PdnDump }
import draughts.variant.{ Variant, Standard }

object Export extends LidraughtsController {

  private def env = Env.game

  private val PngRateLimitGlobal = new lidraughts.memo.RateLimit[String](
    credits = 240,
    duration = 1 minute,
    name = "export PNG global",
    key = "export.png.global"
  )

  def png(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      PngRateLimitGlobal("-", msg = s"${HTTPRequest.lastRemoteAddress(ctx.req).value} ${~HTTPRequest.userAgent(ctx.req)}") {
        lidraughts.mon.export.png.game()
        OptionFuResult(GameRepo game id) { game =>
          env.pngExport fromGame game map pngStream
        }
      }
    }
  }

  def puzzlePng(id: Int) = doPuzzlePng(id, Standard)

  def puzzlePngVariant(id: Int, key: String) = Variant(key) match {
    case Some(variant) if puzzleVariants.contains(variant) => doPuzzlePng(id, variant)
    case _ => Open { implicit ctx => notFound(ctx) }
  }

  private def doPuzzlePng(id: Int, variant: Variant) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      PngRateLimitGlobal("-", msg = HTTPRequest.lastRemoteAddress(ctx.req).value) {
        lidraughts.mon.export.png.puzzle()
        OptionFuResult(Env.puzzle.api.puzzle.find(id, variant)) { puzzle =>
          env.pngExport(
            fen = draughts.format.FEN(puzzle.fenAfterInitialMove | puzzle.fen),
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
