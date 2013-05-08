package controllers

import lila.app._
import views._
import lila.user.{ UserRepo }
import lila.game.{ Pov, GameRepo, PgnRepo, PgnDump }
import lila.round.actorApi.AnalysisAvailable
import lila.round.{ RoomRepo, Room }
import lila.tournament.{ Tournament ⇒ Tourney }

import akka.pattern.ask
import play.api.mvc._
import play.api.http.ContentTypes
import play.api.templates.Html

object Analyse extends LilaController {

  private def env = Env.analyse
  private def bookmarkApi = Env.bookmark.api
  private lazy val pgnDump = (new PgnDump(UserRepo.named)) { gameId ⇒
    routes.Round.watcher(gameId, "white").url
  } _
  // private def roundMessenger = env.round.messenger
  // private def roundSocket = env.round.socket
  // private def roundHubMaster = env.round.hubMaster

  def computer(id: String, color: String) = TODO
  // Auth { implicit ctx ⇒
  //   me ⇒
  //     analyser.getOrGenerate(id, me.id, isGranted(_.MarkEngine)) onComplete {
  //       case Failure(e) ⇒ println(e.getMessage)
  //       case Success(a) ⇒ a.fold(
  //         err ⇒ println("Computer analysis failure: " + err.shows),
  //         analysis ⇒ roundHubMaster ! AnalysisAvailable(id)
  //       )
  //     }
  //     Redirect(routes.Analyse.replay(id, color))
  // }

  def replay(id: String, color: String) = Open { implicit ctx ⇒
    OptionFuOk(GameRepo.pov(id, color)) { pov ⇒
      PgnRepo get id flatMap { pgnString ⇒
        (RoomRepo room pov.gameId map { room ⇒
          html.round.roomInner(room.decodedMessages)
        }) zip
          Env.round.version(pov.gameId) zip
          (bookmarkApi userIdsByGame pov.game) zip
          pgnDump(pov.game, pgnString) zip
          (env.analyser get pov.game.id) zip
          fuccess(none[Tourney]) map {
            // TODO (tournamentRepo byId pov.game.tournamentId) map {
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

  def stats(id: String) = TODO
  // Open { implicit ctx ⇒
  //   IOptionOk(gameRepo game id) { game ⇒
  //     html.analyse.stats(
  //       game = game,
  //       timeChart = new TimeChart(game),
  //       timePies = Pov(game) map { new TimePie(_) })
  //   }
  // }

  def pgn(id: String) = TODO
  // Open { implicit ctx ⇒
  //   IOResult(for {
  //     gameOption ← gameRepo game id
  //     res ← gameOption.fold(io(NotFound("No such game"))) { game ⇒
  //       for {
  //         pgnString ← game.pgnImport.map(_.pgn).fold(pgnRepo get id)(io(_))
  //         content ← game.pgnImport.map(_.pgn).fold(pgnDump(game, pgnString) map (_.toString))(io(_))
  //         filename ← pgnDump filename game
  //       } yield Ok(content).withHeaders(
  //         CONTENT_LENGTH -> content.size.toString,
  //         CONTENT_TYPE -> ContentTypes.TEXT,
  //         CONTENT_DISPOSITION -> ("attachment; filename=" + filename)
  //       )
  //     }
  //   } yield res)
  // }
}
