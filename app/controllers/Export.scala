package controllers

import play.api.mvc.Action

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

  def pdf(id: String) = Open { implicit ctx =>
    OnlyHumans {
      OptionResult(GameRepo game id) { game =>
        Ok.chunked(Enumerator.outputStream(env.pdfExport(game.id))).withHeaders(
          CONTENT_TYPE -> "application/pdf",
          CACHE_CONTROL -> "max-age=7200")
      }
    }
  }

  def png(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebook {
      OptionResult(GameRepo game id) { game =>
        Ok.chunked(Enumerator.outputStream(env.pngExport(game))).withHeaders(
          CONTENT_TYPE -> "image/png",
          CACHE_CONTROL -> "max-age=7200")
      }
    }
  }

  def puzzlePng(id: Int) = Open { implicit ctx =>
    OnlyHumansAndFacebook {
      OptionResult(Env.puzzle.api.puzzle find id) { puzzle =>
        Ok.chunked(Enumerator.outputStream(Env.puzzle.pngExport(puzzle))).withHeaders(
          CONTENT_TYPE -> "image/png",
          CACHE_CONTROL -> "max-age=7200")
      }
    }
  }

  private def OnlyHumans(result: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (HTTPRequest isBot ctx.req) fuccess(NotFound)
    else result

  private def OnlyHumansAndFacebook(result: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (HTTPRequest isFacebookBot ctx.req) result
    else if (HTTPRequest isBot ctx.req) fuccess(NotFound)
    else result
}
