package controllers

import lila.app._
import views._
import lila.user.{ UserRepo }
import lila.game.{ Pov, GameRepo, PgnRepo, PgnDump }
import lila.analyse.{ TimeChart, TimePie }
import lila.round.actorApi.AnalysisAvailable
import lila.round.{ RoomRepo, Room }
import lila.tournament.{ TournamentRepo, Tournament ⇒ Tourney }

import akka.pattern.ask
import play.api.mvc._
import play.api.http.ContentTypes
import play.api.templates.Html
import scala.util.{ Success, Failure }

object Analyse extends LilaController {

  private def env = Env.analyse
  private def bookmarkApi = Env.bookmark.api
  private lazy val pgnDump = new PgnDump(UserRepo.named)
  private lazy val makePgn = pgnDump { gameId ⇒
    routes.Round.watcher(gameId, "white").url
  } _
  private lazy val timeChart = TimeChart(Env.user.usernameOrAnonymous) _

  def computer(id: String, color: String) = Auth { implicit ctx ⇒
    me ⇒
      env.analyser.getOrGenerate(id, me.id, isGranted(_.MarkEngine)) onComplete {
        case Failure(e) ⇒ logwarn(e.getMessage)
        case Success(a) ⇒ a.fold(
          err ⇒ logwarn("Computer analysis failure: " + err.shows),
          _ ⇒ Env.round.socketHub ! AnalysisAvailable(id)
        )
      }
      Redirect(routes.Analyse.replay(id, color)).fuccess
  }

  def replay(id: String, color: String) = Open { implicit ctx ⇒
    OptionFuOk(GameRepo.pov(id, color)) { pov ⇒
      PgnRepo get id flatMap { pgnString ⇒
        (RoomRepo room pov.gameId map { room ⇒
          html.round.roomInner(room.decodedMessages)
        }) zip
          Env.round.version(pov.gameId) zip
          (bookmarkApi userIdsByGame pov.game) zip
          makePgn(pov.game, pgnString) zip
          (env.analyser get pov.game.id) zip
            (pov.game.tournamentId zmap TournamentRepo.byId) map {
            case (((((roomHtml, version), bookmarkers), pgn), analysis), tour) ⇒
              html.analyse.replay(
                pov,
                pgn.toString,
                roomHtml,
                bookmarkers,
                chess.OpeningExplorer openingOf pgnString,
                analysis,
                version,
                tour)
          }
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
      for {
        pgnString ← game.pgnImport.map(_.pgn).fold(PgnRepo get id)(fuccess(_))
        content ← game.pgnImport.map(_.pgn).fold(makePgn(game, pgnString) map (_.toString))(fuccess(_))
        filename ← pgnDump filename game
      } yield Ok(content).withHeaders(
        CONTENT_LENGTH -> content.size.toString,
        CONTENT_TYPE -> ContentTypes.TEXT,
        CONTENT_DISPOSITION -> ("attachment; filename=" + filename)
      )
    }
  }
}
