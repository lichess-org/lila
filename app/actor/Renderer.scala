package lila.app
package actor

import akka.actor._
import play.twirl.api.Html

import lila.game.GameRepo
import lila.user.UserRepo
import views.{ html => V }

private[app] final class Renderer extends Actor {

  def receive = {

    case lila.tv.actorApi.RenderFeaturedJs(game) =>
      sender ! V.game.featuredJs(game)

    case lila.notification.actorApi.RenderNotification(id, from, body) =>
      sender ! V.notification.view(id, from)(Html(body))

    case lila.tournament.actorApi.RemindTournament(tournament) =>
      sender ! spaceless(V.tournament.reminder(tournament))

    case lila.pool.actorApi.RemindPool(pool) =>
      sender ! spaceless(V.pool.reminder(pool))

    case lila.hub.actorApi.setup.RemindChallenge(gameId, from, _) =>
      val replyTo = sender
      (GameRepo game gameId) zip (UserRepo named from) onSuccess {
        case (Some(game), Some(user)) => replyTo ! spaceless(V.setup.challengeNotification(game, user))
      }

    case lila.hub.actorApi.RemindDeployPre =>
      sender ! spaceless(V.notification.deploy("pre"))
    case lila.hub.actorApi.RemindDeployPost =>
      sender ! spaceless(V.notification.deploy("post"))

    case lila.tournament.actorApi.TournamentTable(tours) =>
      sender ! spaceless(V.tournament.enterable(tours))

    case lila.puzzle.RenderDaily(puzzle, fen, lastMove) =>
      sender ! spaceless(V.puzzle.daily(puzzle, fen, lastMove))

    case lila.tv.StreamsOnAir(streams) => sender ! V.tv.streamsOnAir(streams)
  }

  private val spaceRegex = """\s{2,}""".r
  private def spaceless(html: Html) = Html {
    spaceRegex.replaceAllIn(html.body.replace("\\n", " "), " ")
  }
}
