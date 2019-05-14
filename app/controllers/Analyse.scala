package controllers

import play.api.mvc._

import chess.format.FEN
import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Pov, GameRepo, PgnDump }
import lila.round.JsonView.WithFlags
import views._

object Analyse extends LilaController {

  private def env = Env.analyse

  def requestAnalysis(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(GameRepo game id) { game =>
      Env.fishnet.analyser(game, lila.fishnet.Work.Sender(
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

  def replay(pov: Pov, userTv: Option[lila.user.User])(implicit ctx: Context) =
    if (HTTPRequest isBot ctx.req) replayBot(pov)
    else GameRepo initialFen pov.gameId flatMap { initialFen =>
      Game.preloadUsers(pov.game) >> RedirectAtFen(pov, initialFen) {
        (env.analyser get pov.game) zip
          Env.fishnet.api.gameIdExists(pov.gameId) zip
          (pov.game.simulId ?? Env.simul.repo.find) zip
          Round.getWatcherChat(pov.game) zip
          (ctx.noBlind ?? Env.game.crosstableApi.withMatchup(pov.game)) zip
          Env.bookmark.api.exists(pov.game, ctx.me) zip
          Env.api.pgnDump(pov.game, initialFen, analysis = none, PgnDump.WithFlags(clocks = false)) flatMap {
            case analysis ~ analysisInProgress ~ simul ~ chat ~ crosstable ~ bookmarked ~ pgn =>
              Env.api.roundApi.review(pov, lila.api.Mobile.Api.currentVersion,
                tv = userTv.map { u => lila.round.OnUserTv(u.id) },
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
                    Env.analyse.annotator(pgn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status).toString,
                    analysis,
                    analysisInProgress,
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

  def embed(gameId: String, color: String) = Action.async { implicit req =>
    GameRepo.gameWithInitialFen(gameId) flatMap {
      case Some((game, initialFen)) =>
        val pov = Pov(game, chess.Color(color == "white"))
        Env.api.roundApi.embed(pov, lila.api.Mobile.Api.currentVersion,
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
        chess.Replay.plyAtFen(pov.game.pgnMoves, initialFen.map(_.value), pov.game.variant, atFen).fold(
          err => {
            lila.log("analyse").info(s"RedirectAtFen: ${pov.gameId} $atFen $err")
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
    pgn <- Env.api.pgnDump(pov.game, initialFen, analysis, PgnDump.WithFlags(clocks = false))
  } yield Ok(html.analyse.replayBot(
    pov,
    initialFen,
    Env.analyse.annotator(pgn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status).toString,
    simul,
    crosstable
  ))
}
