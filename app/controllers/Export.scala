package controllers

import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Game => GameModel, GameRepo, PgnDump }

object Export extends LilaController {

  private def env = Env.game

  def pgn(id: String) = Open { implicit ctx =>
    lila.mon.export.pgn.game()
    OptionFuResult(GameRepo game id) { game =>
      gameToPgn(
        game,
        asImported = get("as") contains "imported",
        asRaw = get("as").contains("raw")
      ) map { content =>
          Ok(content).withHeaders(
            CONTENT_TYPE -> pgnContentType,
            CONTENT_DISPOSITION -> ("attachment; filename=" + (Env.api.pgnDump filename game))
          )
        } recover {
          case err => NotFound(err.getMessage)
        }
    }
  }

  private def gameToPgn(from: GameModel, asImported: Boolean, asRaw: Boolean): Fu[String] = from match {
    case game if game.playable => fufail("Can't export PGN of game in progress")
    case game => (game.pgnImport.ifTrue(asImported) match {
      case Some(i) => fuccess(i.pgn)
      case None => for {
        initialFen <- GameRepo initialFen game
        pgn <- Env.api.pgnDump(game, initialFen, analysis = none, PgnDump.WithFlags(clocks = !asRaw))
        analysis ← !asRaw ?? (Env.analyse.analyser get game)
      } yield Env.analyse.annotator(pgn, analysis, game.opening, game.winnerColor, game.status, game.clock).toString
    })
  }

  private val PngRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 240,
    duration = 1 minute,
    name = "export PNG global",
    key = "export.png.global"
  )

  def png(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      RequireHttp11 {
        PngRateLimitGlobal("-", msg = s"${HTTPRequest.lastRemoteAddress(ctx.req).value} ${~HTTPRequest.userAgent(ctx.req)}") {
          lila.mon.export.png.game()
          OptionFuResult(GameRepo game id) { game =>
            env.pngExport fromGame game map { stream =>
              Ok.chunked(stream).withHeaders(
                CONTENT_TYPE -> "image/png",
                CACHE_CONTROL -> "max-age=7200"
              )
            }
          }
        }
      }
    }
  }

  def puzzlePng(id: Int) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      RequireHttp11 {
        PngRateLimitGlobal("-", msg = HTTPRequest.lastRemoteAddress(ctx.req).value) {
          lila.mon.export.png.puzzle()
          OptionFuResult(Env.puzzle.api.puzzle find id) { puzzle =>
            env.pngExport(
              fen = chess.format.FEN(puzzle.fenAfterInitialMove | puzzle.fen),
              lastMove = puzzle.initialMove.uci.some,
              check = none,
              orientation = puzzle.color.some,
              logHint = s"puzzle $id"
            ) map { stream =>
                Ok.chunked(stream).withHeaders(
                  CONTENT_TYPE -> "image/png",
                  CACHE_CONTROL -> "max-age=7200"
                )
              }
          }
        }
      }
    }
  }
}
