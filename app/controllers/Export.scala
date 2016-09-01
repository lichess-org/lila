package controllers

import play.api.mvc.Action
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Game => GameModel, GameRepo }
import play.api.http.ContentTypes
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.mvc.Result
import views._

object Export extends LilaController {

  private def env = Env.game

  def pgn(id: String) = Open { implicit ctx =>
    OnlyHumans {
      lila.mon.export.pgn.game()
      OptionFuResult(GameRepo game id) {
        case game if game.playable => NotFound("Can't export PGN of game in progress").fuccess
        case game => (game.pgnImport.ifTrue(get("as") contains "imported") match {
          case Some(i) => fuccess(i.pgn)
          case None => for {
            initialFen <- GameRepo initialFen game
            pgn = Env.api.pgnDump(game, initialFen)
            analysis ← !get("as").contains("raw") ?? (Env.analyse.analyser get game.id)
          } yield Env.analyse.annotator(pgn, analysis, game.opening, game.winnerColor, game.status, game.clock).toString
        }) map { content =>
          Ok(content).withHeaders(
            CONTENT_TYPE -> ContentTypes.TEXT,
            CONTENT_DISPOSITION -> ("attachment; filename=" + (Env.api.pgnDump filename game)))
        }
      }
    }
  }

  private val PdfRateLimitGlobal = new lila.memo.RateLimit(
    credits = 20,
    duration = 1 minute,
    name = "export PDF global")

  def pdf(id: String) = Open { implicit ctx =>
    OnlyHumans {
      PdfRateLimitGlobal("-", msg = HTTPRequest lastRemoteAddress ctx.req) {
        lila.mon.export.pdf()
        OptionResult(GameRepo game id) { game =>
          Ok.chunked(Enumerator.outputStream(env.pdfExport(game.id))).withHeaders(
            CONTENT_TYPE -> "application/pdf",
            CACHE_CONTROL -> "max-age=7200")
        }
      }
    }
  }

  private val PngRateLimitGlobal = new lila.memo.RateLimit(
    credits = 60,
    duration = 1 minute,
    name = "export PGN global")

  def png(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebook {
      PngRateLimitGlobal("-", msg = HTTPRequest lastRemoteAddress ctx.req) {
        lila.mon.export.png.game()
        OptionResult(GameRepo game id) { game =>
          Ok.chunked(Enumerator.outputStream(env.pngExport(game))).withHeaders(
            CONTENT_TYPE -> "image/png",
            CACHE_CONTROL -> "max-age=7200")
        }
      }
    }
  }

  def puzzlePng(id: Int) = Open { implicit ctx =>
    OnlyHumansAndFacebook {
      PngRateLimitGlobal("-", msg = HTTPRequest lastRemoteAddress ctx.req) {
        lila.mon.export.png.puzzle()
        OptionResult(Env.puzzle.api.puzzle find id) { puzzle =>
          Ok.chunked(Enumerator.outputStream(Env.puzzle.pngExport(puzzle))).withHeaders(
            CONTENT_TYPE -> "image/png",
            CACHE_CONTROL -> "max-age=7200")
        }
      }
    }
  }
}
