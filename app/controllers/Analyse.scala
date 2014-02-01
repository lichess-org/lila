package controllers

import scala.util.{ Success, Failure }

import akka.pattern.ask
import play.api.http.ContentTypes
import play.api.mvc._
import play.api.templates.Html

import lila.analyse.{ TimeChart, TimePie, AdvantageChart }
import lila.app._
import lila.game.{ Pov, GameRepo, PgnDump }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.AnalysisAvailable
import lila.tournament.{ TournamentRepo, Tournament ⇒ Tourney }
import lila.user.{ UserRepo }
import views._

object Analyse extends LilaController {

  private def env = Env.analyse
  private def bookmarkApi = Env.bookmark.api
  private lazy val timeChart = TimeChart(Env.user.usernameOrAnonymous) _

  def computer(id: String, color: String) = Auth { implicit ctx ⇒
    me ⇒
      env.analyser.getOrGenerate(id, me.id, isGranted(_.MarkEngine)) effectFold (
        e ⇒ logerr("[analysis] " + e.getMessage),
        _ ⇒ Env.hub.socket.round ! Tell(id, AnalysisAvailable)
      )
      Redirect(routes.Analyse.replay(id, color)).fuccess
  }

  def replay(id: String, color: String) = Open { implicit ctx ⇒
    OptionFuOk(GameRepo.pov(id, color)) { pov ⇒
      Env.round.version(pov.gameId) zip
        Env.game.pgnDump(pov.game) zip
        (env.analyser get pov.game.id) zip
        (pov.game.tournamentId ?? TournamentRepo.byId) zip
        (ctx.isAuth ?? {
          Env.chat.api.userChat find s"${pov.gameId}/w" map (_.forUser(ctx.me).some)
        }) map {
          case ((((version, pgn), analysis), tour), chat) ⇒
            html.analyse.replay(
              pov,
              analysis.fold(pgn)(a ⇒ Env.analyse.annotator(pgn, a)).toString,
              chess.OpeningExplorer openingOf pov.game.pgnMoves,
              analysis,
              analysis filter (_.done) map { a ⇒ AdvantageChart(a.infoAdvices, pov.game.pgnMoves) },
              version,
              chat,
              tour)
        }
    }
  }

  def stats(id: String) = Open { implicit ctx ⇒
    OptionFuOk(GameRepo game id) { game ⇒
      timeChart(game) map { chart ⇒
        html.analyse.stats(
          game = game,
          timeChart = chart,
          timePies = Pov(game) map { new TimePie(_) })
      }
    }
  }

  def pgn(id: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo game id) { game ⇒
      (game.pgnImport match {
        case Some(i) ⇒ fuccess(i.pgn)
        case None ⇒ for {
          pgn ← Env.game.pgnDump(game)
          analysis ← env.analyser get game.id
        } yield analysis.fold(pgn)(a ⇒ Env.analyse.annotator(pgn, a)).toString
      }) flatMap { content ⇒
        Env.game.pgnDump filename game map { filename ⇒
          Ok(content).withHeaders(
            CONTENT_LENGTH -> content.size.toString,
            CONTENT_TYPE -> ContentTypes.TEXT,
            CONTENT_DISPOSITION -> ("attachment; filename=" + filename))
        }
      }
    }
  }

  def fen(id: String) = Open { implicit ctx ⇒
    OptionOk(GameRepo game id) { game ⇒
      Env.round fenUrlWatch game
      chess.format.Forsyth >> game.toChess
    }
  }
}
