package controllers

import scala.concurrent.duration._

import lidraughts.app._
import lidraughts.common.HTTPRequest
import lidraughts.pref.Pref.puzzleVariants
import lidraughts.game.{ Game => GameModel, GameRepo, PdnDump }
import draughts.variant.{ Variant, Standard }

object Export extends LidraughtsController {

  private def env = Env.game

  def pdn(id: String) = Open { implicit ctx =>
    lidraughts.mon.export.pdn.game()
    OptionFuResult(GameRepo game id) { game =>
      gameToPdn(
        game,
        asImported = get("as") contains "imported",
        asRaw = get("as").contains("raw"),
        draughtsResult = ctx.pref.draughtsResult
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

  private def gameToPdn(from: GameModel, asImported: Boolean, asRaw: Boolean, draughtsResult: Boolean): Fu[String] = from match {
    case game if game.playable => fufail("Can't export PDN of game in progress")
    case game => (game.pdnImport.ifTrue(asImported) match {
      case Some(i) => fuccess(i.pdn)
      case None => for {
        initialFen <- GameRepo initialFen game
        pdn <- Env.api.pdnDump(game, initialFen, analysis = none, PdnDump.WithFlags(clocks = !asRaw, draughtsResult = draughtsResult))
        analysis ← !asRaw ?? (Env.analyse.analyser get game)
      } yield Env.analyse.annotator(pdn, analysis, game.opening, game.winnerColor, game.status, game.clock).toString
    })
  }

  private val PngRateLimitGlobal = new lidraughts.memo.RateLimit[String](
    credits = 240,
    duration = 1 minute,
    name = "export PNG global",
    key = "export.png.global"
  )

  def png(id: String) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      RequireHttp11 {
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
  }

  def puzzlePng(id: Int) = doPuzzlePng(id, Standard)

  def puzzlePngVariant(id: Int, key: String) = Variant(key) match {
    case Some(variant) if puzzleVariants.contains(variant) => doPuzzlePng(id, variant)
    case _ => Open { implicit ctx => notFound(ctx) }
  }

  private def doPuzzlePng(id: Int, variant: Variant) = Open { implicit ctx =>
    OnlyHumansAndFacebookOrTwitter {
      RequireHttp11 {
        PngRateLimitGlobal("-", msg = HTTPRequest.lastRemoteAddress(ctx.req).value) {
          lidraughts.mon.export.png.puzzle()
          OptionFuResult(Env.puzzle.api.puzzle.find(id, variant)) { puzzle =>
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

}
