package controllers

import scala.util.{ Success, Failure }

import akka.pattern.ask
import play.api.http.ContentTypes
import play.api.mvc._
import play.twirl.api.Html

import lila.analyse.{ Analysis, TimeChart, AdvantageChart, Accuracy }
import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.evaluation.PlayerAssessments
import lila.game.{ Pov, Game => GameModel, GameRepo, PgnDump }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.AnalysisAvailable
import views._

import chess.Color

object Analyse extends LilaController {

  private def env = Env.analyse
  private def bookmarkApi = Env.bookmark.api
  private val divider = Env.game.cached.Divider

  def requestAnalysis(id: String) = Auth { implicit ctx =>
    me =>
      makeAnalysis(id, me) injectAnyway
        Ok(html.analyse.computing(id))
  }

  private def makeAnalysis(id: String, me: lila.user.User)(implicit ctx: Context) =
    addCallbacks(id) {
      env.analyser.getOrGenerate(id,
        userId = me.id,
        userIp = ctx.req.remoteAddress.some,
        concurrent = isGranted(_.MarkEngine),
        auto = false)
    }

  private[controllers] def addCallbacks(id: String)(analysis: Fu[Analysis]): Fu[Analysis] =
    analysis andThen {
      case Failure(e: lila.analyse.ConcurrentAnalysisException) => Env.hub.socket.round ! Tell(id, AnalysisAvailable)
      case Failure(err)                                         => logerr("[analysis] " + err.getMessage)
      case Success(analysis) if analysis.done                   => Env.hub.socket.round ! Tell(id, AnalysisAvailable)
    }

  def postAnalysis(id: String) = Action.async(parse.text) { req =>
    env.analyser.complete(id, req.body, req.remoteAddress) recover {
      case e: lila.common.LilaException => logwarn(s"AI ${req.remoteAddress} ${e.message}")
    } andThenAnyway {
      Env.hub.socket.round ! Tell(id, AnalysisAvailable)
    } inject Ok
  }

  def postAnalysisErr(id: String) = Action(parse.text) { req =>
    env.analyser.completeErr(id, req.body, req.remoteAddress)
    logwarn(s"AI failure ${req.remoteAddress} ${req.body}")
    Ok
  }

  def replay(pov: Pov, userTv: Option[lila.user.User])(implicit ctx: Context) =
    if (HTTPRequest isBot ctx.req) replayBot(pov)
    else GameRepo initialFen pov.game.id flatMap { initialFen =>
      RedirectAtFen(pov, initialFen) {
        (env.analyser get pov.game.id) zip
          (pov.game.simulId ?? Env.simul.repo.find) zip
          Env.game.crosstableApi(pov.game) flatMap {
            case ((analysis, simul), crosstable) =>
              val pgn = Env.api.pgnDump(pov.game, initialFen)
              Env.api.roundApi.watcher(pov, lila.api.Mobile.Api.currentVersion,
                tv = none,
                analysis.map(pgn -> _),
                initialFenO = initialFen.some,
                withMoveTimes = true,
                withOpening = true) map { data =>
                  Ok(html.analyse.replay(
                    pov,
                    data,
                    initialFen,
                    Env.analyse.annotator(pgn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status, pov.game.clock).toString,
                    analysis,
                    analysis filter (_.done) map { a => AdvantageChart(a.infoAdvices, pov.game.pgnMoves, pov.game.startedAtTurn) },
                    simul,
                    new TimeChart(pov.game, pov.game.pgnMoves),
                    crosstable,
                    userTv,
                    divider(pov.game, initialFen)))
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
            loginfo(s"RedirectAtFen: $err")
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
