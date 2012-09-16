package controllers

import lila._
import views._
import analyse._
import round.AnalysisAvailable

import play.api.mvc._
import play.api.http.ContentTypes
import play.api.templates.Html
import play.api.libs.concurrent._
import scalaz.effects._

object Analyse extends LilaController {

  private def gameRepo = env.game.gameRepo
  private def pgnDump = env.analyse.pgnDump
  private def openingExplorer = chess.OpeningExplorer
  private def bookmarkApi = env.bookmark.api
  private def roundMessenger = env.round.messenger
  private def roundSocket = env.round.socket
  private def roundHubMaster = env.round.hubMaster
  private def analyser = env.analyse.analyser
  private def tournamentRepo = env.tournament.repo

  def computer(id: String, color: String) = Auth { implicit ctx ⇒
    me ⇒
      analyser.getOrGenerate(id, me.id, isGranted(_.MarkEngine)) onComplete {
        case Left(e) ⇒ println(e.getMessage)
        case Right(a) ⇒ a.fold(
          err ⇒ println("Computer analysis failure: " + err.shows),
          analysis ⇒ roundHubMaster ! AnalysisAvailable(id)
        )
      }
      Redirect(routes.Analyse.replay(id, color))
  }

  def replay(id: String, color: String) = Open { implicit ctx ⇒
    IOptionIOk(gameRepo.pov(id, color)) { pov ⇒
      for {
        roomHtml ← roundMessenger renderWatcher pov.game
        bookmarkers ← bookmarkApi usersByGame pov.game
        pgn ← pgnDump >> pov.game
        analysis ← analyser get pov.game.id
        tour ← tournamentRepo byId pov.game.tournamentId
      } yield html.analyse.replay(
        pov,
        pgn.toString,
        Html(roomHtml),
        bookmarkers,
        openingExplorer openingOf pov.game.pgn,
        analysis,
        roundSocket blockingVersion pov.gameId,
        tour)
    }
  }

  def stats(id: String) = Open { implicit ctx ⇒
    IOptionOk(gameRepo game id) { game ⇒
      html.analyse.stats(
        game = game,
        timeChart = new TimeChart(game))
    }
  }

  def pgn(id: String) = Open { implicit ctx ⇒
    IOResult(for {
      gameOption ← gameRepo game id
      res ← gameOption.fold(
        game ⇒ for {
          pgnObj ← pgnDump >> game
          content = pgnObj.toString
          filename ← pgnDump filename game
        } yield Ok(content).withHeaders(
          CONTENT_LENGTH -> content.size.toString,
          CONTENT_TYPE -> ContentTypes.TEXT,
          CONTENT_DISPOSITION -> ("attachment; filename=" + filename)
        ),
        io(NotFound("No such game"))
      )
    } yield res)
  }
}
