package controllers

import lila._
import views._
import analyse._

import play.api.mvc._
import play.api.http.ContentTypes
import play.api.templates.Html
import scalaz.effects._

object Analyse extends LilaController {

  def gameRepo = env.game.gameRepo
  def pgnDump = env.analyse.pgnDump
  def openingExplorer = chess.OpeningExplorer
  def bookmarkApi = env.bookmark.api
  def roundMessenger = env.round.messenger
  def roundSocket = env.round.socket

  def replay(id: String, color: String) = Open { implicit ctx ⇒
    IOptionIOk(gameRepo.pov(id, color)) { pov ⇒
      for {
        roomHtml ← roundMessenger renderWatcher pov.game
        bookmarkers ← bookmarkApi usersByGame pov.game
      } yield html.analyse.replay(
        pov,
        Html(roomHtml),
        bookmarkers,
        openingExplorer openingOf pov.game.pgn,
        roundSocket blockingVersion pov.gameId)
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
          content ← pgnDump >> game
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
