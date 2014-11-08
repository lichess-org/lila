package controllers

import scala.util.{ Success, Failure }

import akka.pattern.ask
import play.api.http.ContentTypes
import play.api.mvc._
import play.twirl.api.Html

import lila.analyse.{ Analysis, TimeChart, AdvantageChart }
import lila.api.Context
import lila.app._
import lila.game.{ Pov, Game => GameModel, GameRepo, PgnDump }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.AnalysisAvailable
import views._

object Analyse extends LilaController {

  private def env = Env.analyse
  private def bookmarkApi = Env.bookmark.api

  def requestAnalysis(id: String) = Auth { implicit ctx =>
    me =>
      makeAnalysis(id, me) injectAnyway
        Ok(html.analyse.computing())
  }

  def betterAnalysis(id: String, color: String) = Auth { implicit ctx =>
    me =>
      makeAnalysis(id, me) injectAnyway
        Redirect(routes.Round.watcher(id, color))
  }

  private def makeAnalysis(id: String, me: lila.user.User)(implicit ctx: Context) =
    addCallbacks(id) {
      env.analyser.getOrGenerate(id, me.id, concurrent = isGranted(_.MarkEngine), auto = false)
    }

  private[controllers] def addCallbacks(id: String)(analysis: Fu[Analysis]): Fu[Analysis] =
    analysis andThen {
      case Failure(e: lila.analyse.ConcurrentAnalysisException) => Env.hub.socket.round ! Tell(id, AnalysisAvailable)
      case Failure(err)                                         => logerr("[analysis] " + err.getMessage)
      case Success(analysis) if analysis.done                   => Env.hub.socket.round ! Tell(id, AnalysisAvailable)
    }

  def postAnalysis(id: String) = Action(parse.text) { req =>
    env.analyser.complete(id, req.body) >>- {
      Env.hub.socket.round ! Tell(id, AnalysisAvailable)
    }
    Ok
  }

  def replay(pov: Pov)(implicit ctx: Context) =
    Env.game.pgnDump(pov.game) zip
      (env.analyser get pov.game.id) zip
      (pov.game.tournamentId ?? lila.tournament.TournamentRepo.byId) zip
      lila.game.Divider(pov.game) zip
      Env.game.crosstableApi(pov.game) flatMap {
        case ((((pgn, analysis), tour), division), crosstable) =>
          Env.api.roundApi.watcher(pov, Env.api.version, tv = none, analysis.map(pgn -> _)) map { data =>
            val opening = gameOpening(pov.game)
            Ok(html.analyse.replay(
              pov,
              data,
              Env.analyse.annotator(pgn, analysis, opening, pov.game.winnerColor, pov.game.status, pov.game.clock).toString,
              opening,
              analysis,
              analysis filter (_.done) map { a => AdvantageChart(a.infoAdvices, pov.game.pgnMoves) },
              tour,
              new TimeChart(pov.game, pov.game.pgnMoves),
              crosstable,
              division))
          }
      }

  private def gameOpening(game: GameModel) =
    if (game.fromPosition || game.variant.exotic) none
    else chess.OpeningExplorer openingOf game.pgnMoves
}
