package controllers

import play.api.mvc._

import draughts.format.FEN
import lidraughts.api.Context
import lidraughts.app._
import lidraughts.common.HTTPRequest
import lidraughts.game.{ Pov, GameRepo, PdnDump }
import lidraughts.round.JsonView.WithFlags
import views._

object Analyse extends LidraughtsController {

  private def env = Env.analyse

  /*def requestAnalysis(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(GameRepo game id) { game =>
      Env.fishnet.analyser(game, lidraughts.fishnet.Work.Sender(
        userId = me.id.some,
        ip = HTTPRequest.lastRemoteAddress(ctx.req).some,
        mod = isGranted(_.Hunter),
        system = false
      )) map {
        case true => NoContent
        case false => Unauthorized
      }
    }
  }*/

  def replay(pov: Pov, userTv: Option[lidraughts.user.User])(implicit ctx: Context) =
    if (HTTPRequest isBot ctx.req) replayBot(pov)
    else GameRepo initialFen pov.game.id flatMap { initialFen =>
      Game.preloadUsers(pov.game) >> RedirectAtFen(pov, initialFen) {
        (env.analyser get pov.game.id) zip
          (pov.game.simulId ?? Env.simul.repo.find) zip
          Round.getWatcherChat(pov.game) zip
          Env.game.crosstableApi.withMatchup(pov.game) zip
          Env.bookmark.api.exists(pov.game, ctx.me) zip
          Env.api.pdnDump(pov.game, initialFen, PdnDump.WithFlags(clocks = false)) flatMap {
            case analysis ~ simul ~ chat ~ crosstable ~ bookmarked ~ pdn =>
              Env.api.roundApi.review(pov, lidraughts.api.Mobile.Api.currentVersion,
                tv = userTv.map { u => lidraughts.round.OnUserTv(u.id) },
                analysis,
                initialFenO = initialFen.map(FEN).some,
                withFlags = WithFlags(
                  movetimes = true,
                  clocks = true,
                  division = true,
                  opening = true
                )) map { data =>
                  Ok(html.analyse.replay(
                    pov,
                    data,
                    initialFen,
                    Env.analyse.annotator(pdn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status, pov.game.clock).toString,
                    analysis,
                    simul,
                    crosstable,
                    userTv,
                    chat,
                    bookmarked = bookmarked
                  ))
                }
          }
      }
    }

  def embed(gameId: String, color: String) = Open { implicit ctx =>
    GameRepo.gameWithInitialFen(gameId) flatMap {
      case Some((game, initialFen)) =>
        val pov = Pov(game, draughts.Color(color == "white"))
        Env.api.roundApi.review(pov, lidraughts.api.Mobile.Api.currentVersion,
          initialFenO = initialFen.some,
          withFlags = WithFlags(opening = true)) map { data =>
            Ok(html.analyse.embed(pov, data))
          }
      case _ => fuccess(NotFound(html.analyse.embedNotFound()))
    }
  }

  private def RedirectAtFen(pov: Pov, initialFen: Option[String])(or: => Fu[Result])(implicit ctx: Context) =
    get("fen").fold(or) { atFen =>
      val url = routes.Round.watcher(pov.gameId, pov.color.name)
      fuccess {
        draughts.Replay.plyAtFen(pov.game.pdnMoves, initialFen, pov.game.variant, atFen).fold(
          err => {
            lidraughts.log("analyse").info(s"RedirectAtFen: ${pov.gameId} $atFen $err")
            Redirect(url)
          },
          ply => Redirect(s"$url#$ply")
        )
      }
    }

  private def replayBot(pov: Pov)(implicit ctx: Context) = for {
    initialFen <- GameRepo initialFen pov.game.id
    analysis <- env.analyser get pov.game.id
    simul <- pov.game.simulId ?? Env.simul.repo.find
    crosstable <- Env.game.crosstableApi.withMatchup(pov.game)
    pdn <- Env.api.pdnDump(pov.game, initialFen, PdnDump.WithFlags(clocks = false))
  } yield Ok(html.analyse.replayBot(
    pov,
    initialFen,
    Env.analyse.annotator(pdn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status, pov.game.clock).toString,
    analysis,
    simul,
    crosstable
  ))
}
