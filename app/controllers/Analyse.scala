package controllers

import scala.util.{ Success, Failure }

import akka.pattern.ask
import play.api.http.ContentTypes
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.evaluation.PlayerAssessments
import lila.game.{ Pov, Game => GameModel, GameRepo, PgnDump }
import lila.hub.actorApi.map.Tell
import views._

import chess.Color

object Analyse extends LilaController {

  private def env = Env.analyse
  private def bookmarkApi = Env.bookmark.api
  private val divider = Env.game.divider

  def requestAnalysis(id: String) = Auth { implicit ctx =>
    me =>
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
          Env.game.crosstableApi(pov.game) flatMap {
            case (((analysis, analysisInProgress), simul), crosstable) =>
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
                    userTv))
                }
          }
      }
    }

  private def RedirectAtFen(pov: Pov, initialFen: Option[String])(or: => Fu[Result])(implicit ctx: Context) =
    get("fen").fold(or) { atFen =>
      val url = routes.Round.watcher(pov.gameId, pov.color.name)
      fuccess {
        chess.Replay.plyAtFen(pov.game.pgnMoves, initialFen, pov.game.variant, atFen).fold(
          err => {
            lila.log("analyse").info(s"RedirectAtFen: http://lichess.org/${pov.gameId} $atFen $err")
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
