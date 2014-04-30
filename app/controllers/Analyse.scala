package controllers

import scala.util.{ Success, Failure }

import akka.pattern.ask
import play.api.http.ContentTypes
import play.api.mvc._
import play.api.templates.Html

import lila.analyse.{ TimeChart, AdvantageChart }
import lila.api.Context
import lila.app._
import lila.game.{ Pov, GameRepo, PgnDump }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.AnalysisAvailable
import lila.tournament.{ TournamentRepo, Tournament => Tourney }
import lila.user.{ UserRepo }
import views._

object Analyse extends LilaController {

  private def env = Env.analyse
  private def bookmarkApi = Env.bookmark.api

  def requestAnalysis(id: String) = Auth { implicit ctx =>
    me =>
      env.analyser.getOrGenerate(id, me.id, isGranted(_.MarkEngine)) onComplete {
        case Failure(err)                       => logerr("[analysis] " + err.getMessage)
        case Success(analysis) if analysis.done => Env.hub.socket.round ! Tell(id, AnalysisAvailable)
        case _                                  =>
      }
      fuccess(Ok(html.analyse.computing()))
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
      (pov.game.tournamentId ?? TournamentRepo.byId) zip
      Env.game.crosstableApi(pov.game) zip
      (ctx.isAuth ?? {
        Env.chat.api.userChat find s"${pov.gameId}/w" map (_.forUser(ctx.me).some)
      }) map {
        case (((((version, pgn), analysis), tour), crosstable), chat) =>
          Ok(html.analyse.replay(
            pov,
            analysis.fold(pgn)(a => Env.analyse.annotator(pgn, a)).toString,
            chess.OpeningExplorer openingOf pov.game.pgnMoves,
            analysis,
            analysis filter (_.done) map { a => AdvantageChart(a.infoAdvices, pov.game.pgnMoves) },
            version,
            chat,
            tour,
            new TimeChart(pov.game, pov.game.pgnMoves),
            crosstable))
      }

  def pgn(id: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game id) { game =>
      (game.pgnImport match {
        case Some(i) => fuccess(i.pgn)
        case None => for {
          pgn ← Env.game.pgnDump(game)
          analysis ← env.analyser getDone game.id
        } yield analysis.fold(pgn)(a => Env.analyse.annotator(pgn, a)).toString
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

  def fen(id: String) = Open { implicit ctx =>
    OptionOk(GameRepo game id) { game =>
      Env.round fenUrlWatch game
      chess.format.Forsyth >> game.toChess
    }
  }
}
