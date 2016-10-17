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
      OptionFuResult(GameRepo game id) { game =>
        gameToPgn(
          game,
          asImported = get("as") contains "imported",
          asRaw = get("as").contains("raw")) map { content =>
            Ok(content).withHeaders(
              CONTENT_TYPE -> ContentTypes.TEXT,
              CONTENT_DISPOSITION -> ("attachment; filename=" + (Env.api.pgnDump filename game)))
          } recover {
            case err => NotFound(err.getMessage)
          }
      }
    }
  }

  private def gameToPgn(from: GameModel, asImported: Boolean, asRaw: Boolean): Fu[String] = from match {
    case game if game.playable => fufail("Can't export PGN of game in progress")
    case game => (game.pgnImport.ifTrue(asImported) match {
      case Some(i) => fuccess(i.pgn)
      case None => for {
        initialFen <- GameRepo initialFen game
        pgn = Env.api.pgnDump(game, initialFen)
        analysis ← !asRaw ?? (Env.analyse.analyser get game.id)
      } yield Env.analyse.annotator(pgn, analysis, game.opening, game.winnerColor, game.status, game.clock).toString
    })
  }

  private val PdfRateLimitGlobal = new lila.memo.RateLimit(
    credits = 20,
    duration = 1 minute,
    name = "export PDF global",
    key = "export.pdf.global")

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
    name = "export PGN global",
    key = "export.pgn.global")

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

  def visualizer(id: String) = Open { implicit ctx =>
    OnlyHumans {
      OptionFuResult(GameRepo game id) { game =>
        gameToPgn(game, asImported = false, asRaw = false) map { pgn =>
          lila.mon.export.visualizer()
          Redirect {
            import lila.api.Env.current.Net._
            val base = s"$Protocol$AssetDomain/assets"
            val encoded = java.net.URLEncoder.encode(pgn.toString, "UTF-8")
            s"$base/visualizer/index_lichess.html?pgn=$encoded"
          }
        } recoverWith {
          case _: Exception => notFound
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
