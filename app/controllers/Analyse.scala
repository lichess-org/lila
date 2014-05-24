package controllers

import scala.util.{ Success, Failure }

import akka.pattern.ask
import play.api.http.ContentTypes
import play.api.mvc._
import play.api.templates.Html

import lila.analyse.{ TimeChart, AdvantageChart }
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
      makeAnalysis(id, me) inject
        Ok(html.analyse.computing())
  }

  def betterAnalysis(id: String, color: String) = Auth { implicit ctx =>
    me =>
      makeAnalysis(id, me) inject
        Redirect(routes.Round.watcher(id, color))
  }

  private def makeAnalysis(id: String, me: lila.user.User)(implicit ctx: Context) =
    env.analyser.getOrGenerate(id, me.id, isGranted(_.MarkEngine)) andThen {
      case Failure(e: lila.analyse.ConcurrentAnalysisException) => BadRequest(e.getMessage)
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
    Env.round.version(pov.gameId) zip
      Env.game.pgnDump(pov.game) zip
      (env.analyser get pov.game.id) zip
      (pov.game.tournamentId ?? lila.tournament.TournamentRepo.byId) zip
      Env.game.crosstableApi(pov.game) zip
      (ctx.isAuth ?? {
        Env.chat.api.userChat find s"${pov.gameId}/w" map (_.forUser(ctx.me).some)
      }) map {
        case (((((version, pgn), analysis), tour), crosstable), chat) => {
          val opening = gameOpening(pov.game)
          Ok(html.analyse.replay(
            pov,
            Env.analyse.annotator(pgn, analysis, opening).toString,
            opening,
            analysis,
            analysis filter (_.done) map { a => AdvantageChart(a.infoAdvices, pov.game.pgnMoves) },
            version,
            chat,
            tour,
            new TimeChart(pov.game, pov.game.pgnMoves),
            crosstable))
        }
      }

  def pgn(id: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game id) { game =>
      (game.pgnImport match {
        case Some(i) => fuccess(i.pgn)
        case None => for {
          pgn ← Env.game.pgnDump(game)
          analysis ← env.analyser getDone game.id
        } yield Env.analyse.annotator(pgn, analysis, gameOpening(game)).toString
      }) flatMap { content =>
        Env.game.pgnDump filename game map { filename =>
          Ok(content).withHeaders(
            CONTENT_LENGTH -> content.size.toString,
            CONTENT_TYPE -> ContentTypes.TEXT,
            CONTENT_DISPOSITION -> ("attachment; filename=" + filename))
        }
      }
    }
  }

  private def gameOpening(game: GameModel) =
    if (game.fromPosition || game.variant.exotic) none
    else chess.OpeningExplorer openingOf game.pgnMoves

  def fen(id: String) = Open { implicit ctx =>
    OptionOk(GameRepo game id) { game =>
      Env.round fenUrlWatch game
      chess.format.Forsyth >> game.toChess
    }
  }
}
