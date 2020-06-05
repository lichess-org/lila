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

  def requestAnalysis(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(GameRepo game id) { game =>
      Env.draughtsnet.analyser(game, lidraughts.draughtsnet.Work.Sender(
        userId = me.id.some,
        ip = HTTPRequest.lastRemoteAddress(ctx.req).some,
        mod = isGranted(_.Hunter) || isGranted(_.Relay),
        system = false
      )) map {
        case true => NoContent
        case false => Unauthorized
      }
    }
  }

  def replay(pov: Pov, userTv: Option[lidraughts.user.User], userTvGameId: Option[String] = None)(implicit ctx: Context) =
    if (HTTPRequest isBot ctx.req) replayBot(pov)
    else GameRepo initialFen pov.gameId flatMap { initialFen =>
      val pdnFlags = PdnDump.WithFlags(clocks = false, draughtsResult = ctx.pref.draughtsResult, algebraic = ctx.pref.isAlgebraic(pov.game.variant))
      Game.preloadUsers(pov.game) >> RedirectAtFen(pov, initialFen) {
        (env.analyser get pov.game) zip
          Env.draughtsnet.api.gameIdExists(pov.gameId) zip
          (pov.game.simulId ?? Env.simul.repo.find) zip
          Round.getWatcherChat(pov.game) zip
          (ctx.noBlind ?? Env.game.crosstableApi.withMatchup(pov.game)) zip
          Env.bookmark.api.exists(pov.game, ctx.me) zip
          Env.api.pdnDump(pov.game, initialFen, analysis = none, pdnFlags) zip
          isGranted(_.Hunter).??(Env.mod.cheatList.get(pov.game).map(some)) flatMap {
            case analysis ~ analysisInProgress ~ simul ~ chat ~ crosstable ~ bookmarked ~ pdn ~ onCheatList =>
              Env.api.roundApi.review(pov, lidraughts.api.Mobile.Api.currentVersion,
                tv = userTv.map { u => lidraughts.round.OnUserTv(u.id, userTvGameId) },
                analysis,
                initialFenO = initialFen.some,
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
                    Env.analyse.annotator(pdn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status).toString,
                    analysis,
                    analysisInProgress,
                    simul,
                    crosstable,
                    userTv,
                    chat,
                    bookmarked = bookmarked,
                    onCheatList = onCheatList
                  ))
                }
          }
      }
    }

  def embed(gameId: String, color: String) = Action.async { implicit req =>
    GameRepo.gameWithInitialFen(gameId) flatMap {
      case Some((game, initialFen)) =>
        val pov = Pov(game, draughts.Color(color == "white"))
        Env.api.roundApi.embed(pov, lidraughts.api.Mobile.Api.currentVersion,
          initialFenO = initialFen.some,
          withFlags = WithFlags(opening = true)) map { data =>
            Ok(html.analyse.embed(pov, data))
          }
      case _ => fuccess(NotFound(html.analyse.embed.notFound))
    }
  }

  private def RedirectAtFen(pov: Pov, initialFen: Option[FEN])(or: => Fu[Result])(implicit ctx: Context) =
    get("fen").fold(or) { atFen =>
      val url = routes.Round.watcher(pov.gameId, pov.color.name)
      fuccess {
        draughts.Replay.plyAtFen(pov.game.pdnMoves, initialFen.map(_.value), pov.game.variant, atFen).fold(
          err => {
            lidraughts.log("analyse").info(s"RedirectAtFen: ${pov.gameId} $atFen $err")
            Redirect(url)
          },
          ply => Redirect(s"$url#$ply")
        )
      }
    }

  private def replayBot(pov: Pov)(implicit ctx: Context) = for {
    initialFen <- GameRepo initialFen pov.gameId
    analysis <- env.analyser get pov.game
    simul <- pov.game.simulId ?? Env.simul.repo.find
    crosstable <- Env.game.crosstableApi.withMatchup(pov.game)
    pdn <- Env.api.pdnDump(pov.game, initialFen, analysis, PdnDump.WithFlags(clocks = false))
  } yield Ok(html.analyse.replayBot(
    pov,
    initialFen,
    Env.analyse.annotator(pdn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status).toString,
    simul,
    crosstable
  ))
}
