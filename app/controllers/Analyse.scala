package controllers

import scala.concurrent.duration._

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Pov, GameRepo }
import views._

object Analyse extends LilaController {

  private def env = Env.analyse
  private def bookmarkApi = Env.bookmark.api
  private val divider = Env.game.divider

  def requestAnalysis(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(GameRepo game id) { game =>
      Env.fishnet.analyser(game, lila.fishnet.Work.Sender(
        userId = me.id.some,
        ip = HTTPRequest.lastRemoteAddress(ctx.req).some,
        mod = isGranted(_.Hunter),
        system = false)) map {
        case true  => Ok
        case false => Unauthorized
      }
    }
  }

  def replay(pov: Pov, userTv: Option[lila.user.User])(implicit ctx: Context) =
    if (HTTPRequest isBot ctx.req) replayBot(pov)
    else GameRepo initialFen pov.game.id flatMap { initialFen =>
      RedirectAtFen(pov, initialFen) {
        (env.analyser get pov.game.id) zip
          Env.fishnet.api.prioritaryAnalysisInProgress(pov.game.id) zip
          (pov.game.simulId ?? Env.simul.repo.find) zip
          Round.getWatcherChat(pov.game) zip
          Env.game.crosstableApi(pov.game) zip
          Env.bookmark.api.exists(pov.game, ctx.me) flatMap {
            case (((((analysis, analysisInProgress), simul), chat), crosstable), bookmarked) =>
              val pgn = Env.api.pgnDump(pov.game, initialFen)
              Env.api.roundApi.review(pov, lila.api.Mobile.Api.currentVersion,
                tv = none,
                analysis,
                initialFenO = initialFen.some,
                withMoveTimes = true,
                withDivision = true,
                withOpening = true) map { data =>
                Ok(html.analyse.replay(
                  pov,
                  data,
                  initialFen,
                  Env.analyse.annotator(pgn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status, pov.game.clock).toString,
                  analysis,
                  analysisInProgress,
                  simul,
                  crosstable,
                  userTv,
                  chat,
                  bookmarked = bookmarked))
              }
          }
      }
    }

  def embed(gameId: String, color: String) = Open { implicit ctx =>
    GameRepo.gameWithInitialFen(gameId) flatMap {
      case Some((game, initialFen)) =>
        val pov = Pov(game, chess.Color(color == "white"))
        Env.api.roundApi.review(pov, lila.api.Mobile.Api.currentVersion,
          initialFenO = initialFen.map(_.value).some,
          withMoveTimes = false,
          withDivision = false,
          withOpening = true) map { data =>
          Ok(html.analyse.embed(pov, data))
        }
      case _ => fuccess(NotFound(html.analyse.embedNotFound()))
    }
  }

  private def RedirectAtFen(pov: Pov, initialFen: Option[String])(or: => Fu[Result])(implicit ctx: Context) =
    get("fen").fold(or) { atFen =>
      val url = routes.Round.watcher(pov.gameId, pov.color.name)
      fuccess {
        chess.Replay.plyAtFen(pov.game.pgnMoves, initialFen, pov.game.variant, atFen).fold(
          err => {
            lila.log("analyse").info(s"RedirectAtFen: ${pov.gameId} $atFen $err")
            Redirect(url)
          },
          ply => Redirect(s"$url#$ply"))
      }
    }

  private def replayBot(pov: Pov)(implicit ctx: Context) =
    GameRepo initialFen pov.game.id flatMap { initialFen =>
      (env.analyser get pov.game.id) zip
        (pov.game.simulId ?? Env.simul.repo.find) zip
        Env.game.crosstableApi(pov.game) map {
          case ((analysis, simul), crosstable) =>
            val pgn = Env.api.pgnDump(pov.game, initialFen)
            Ok(html.analyse.replayBot(
              pov,
              initialFen,
              Env.analyse.annotator(pgn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status, pov.game.clock).toString,
              analysis,
              simul,
              crosstable))
        }
    }
}
