package controllers

import scala.concurrent.duration._

import lidraughts.app._
import lidraughts.common.HTTPRequest
import lidraughts.game.{ Game => GameModel, GameRepo, PdnDump }

object Export extends LidraughtsController {

  private def env = Env.game

  def pdn(id: String) = Open { implicit ctx =>
    lidraughts.mon.export.pdn.game()
    OptionFuResult(GameRepo game id) { game =>
      gameToPdn(
        game,
        asImported = get("as") contains "imported",
        asRaw = get("as").contains("raw")
      ) map { content =>
          Ok(content).withHeaders(
            CONTENT_TYPE -> pdnContentType,
            CONTENT_DISPOSITION -> ("attachment; filename=" + (Env.api.pdnDump filename game))
          )
        } recover {
          case err => NotFound(err.getMessage)
        }
    }
  }

  private def gameToPdn(from: GameModel, asImported: Boolean, asRaw: Boolean): Fu[String] = from match {
    case game if game.playable => fufail("Can't export PDN of game in progress")
    case game => (game.pdnImport.ifTrue(asImported) match {
      case Some(i) => fuccess(i.pdn)
      case None => for {
        initialFen <- GameRepo initialFen game
        pdn <- Env.api.pdnDump(game, initialFen, PdnDump.WithFlags(clocks = !asRaw))
        analysis ← !asRaw ?? (Env.analyse.analyser get game.id)
      } yield Env.analyse.annotator(pdn, analysis, game.opening, game.winnerColor, game.status, game.clock).toString
    })
  }

  private val PngRateLimitGlobal = new lidraughts.memo.RateLimit[String](
    credits = 120,
    duration = 1 minute,
    name = "export PDN global",
    key = "export.pdn.global"
  )

  def png(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      PngRateLimitGlobal("-", msg = s"${HTTPRequest.lastRemoteAddress(ctx.req).value} ${~HTTPRequest.userAgent(ctx.req)}") {
        lidraughts.mon.export.png.game()
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

  def puzzlePng(id: Int) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      PngRateLimitGlobal("-", msg = HTTPRequest.lastRemoteAddress(ctx.req).value) {
        lidraughts.mon.export.png.puzzle()
        OptionFuResult(Env.puzzle.api.puzzle find id) { puzzle =>
          env.pngExport(
            fen = draughts.format.FEN(puzzle.fenAfterInitialMove | puzzle.fen),
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
