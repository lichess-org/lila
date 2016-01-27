package lila.app
package actor

import akka.actor._
import play.twirl.api.Html

import lila.game.{ GameRepo, Pov }
import lila.user.UserRepo
import views.{ html => V }

private[app] final class Renderer extends Actor {

  def receive = {

    case lila.tv.actorApi.RenderFeaturedJs(game) =>
      sender ! V.game.featuredJs(Pov first game)

    case lila.notification.actorApi.RenderNotification(id, from, body) =>
      sender ! V.notification.view(id, from)(Html(body))

    case lila.tournament.actorApi.RemindTournament(tournament, _) =>
      sender ! spaceless(V.tournament.reminder(tournament))

    case lila.hub.actorApi.RemindDeployPre =>
      sender ! spaceless(V.notification.deploy("pre"))
    case lila.hub.actorApi.RemindDeployPost =>
      sender ! spaceless(V.notification.deploy("post"))

    case lila.tournament.actorApi.TournamentTable(tours) =>
      sender ! spaceless(V.tournament.enterable(tours))

    case lila.simul.actorApi.SimulTable(simuls) =>
      sender ! spaceless(V.simul.allCreated(simuls))

    case lila.puzzle.RenderDaily(puzzle, fen, lastMove) =>
      sender ! spaceless(V.puzzle.daily(puzzle, fen, lastMove))

    case lila.tv.StreamsOnAir(streams) => sender ! V.tv.streamsOnAir(streams)
  }

  private val spaceRegex = """\s{2,}""".r
  private def spaceless(html: Html) = Html {
    spaceRegex.replaceAllIn(html.body.replace("\\n", " "), " ")
  }
}
