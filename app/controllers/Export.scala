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
    NotForBots {
      OptionFuResult(GameRepo game id) { game =>
        (game.pgnImport.ifTrue(~get("as") == "imported") match {
          case Some(i) => fuccess(i.pgn)
          case None => for {
            initialFen <- GameRepo initialFen game
            pgn = Env.game.pgnDump(game, initialFen)
            analysis ← (~get("as") != "raw") ?? (Env.analyse.analyser getDone game.id)
          } yield Env.analyse.annotator(pgn, analysis, gameOpening(game), game.winnerColor, game.status, game.clock).toString
        }) map { content =>
          Ok(content).withHeaders(
            CONTENT_TYPE -> ContentTypes.TEXT,
            CONTENT_DISPOSITION -> ("attachment; filename=" + (Env.game.pgnDump filename game)))
        }
      }
    }
  }

  def pdf(id: String) = Open { implicit ctx =>
    NotForBots {
      OptionResult(GameRepo game id) { game =>
        Ok.chunked(Enumerator.outputStream(env.pdfExport(game.id))).withHeaders(
          CONTENT_TYPE -> "application/pdf",
          CACHE_CONTROL -> "max-age=7200")
      }
    }
  }

  def png(id: String) = Open { implicit ctx =>
    NotForBots {
      OptionResult(GameRepo game id) { game =>
        Ok.chunked(Enumerator.outputStream(env.pngExport(game))).withHeaders(
          CONTENT_TYPE -> "image/png",
          CACHE_CONTROL -> "max-age=7200")
      }
    }
  }

  private def NotForBots(result: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (HTTPRequest isBot ctx.req) fuccess(NotFound)
    else result

  private def gameOpening(game: GameModel) =
    if (game.fromPosition || game.variant.exotic) none
    else chess.OpeningExplorer openingOf game.pgnMoves
}
