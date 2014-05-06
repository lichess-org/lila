package lila.app
package actor

import akka.actor._
import play.api.templates.Html

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
      sender ! V.tournament.reminder(tournament)

    case lila.hub.actorApi.setup.RemindChallenge(gameId, from, _) =>
      val replyTo = sender
      (GameRepo game gameId) zip (UserRepo named from) onSuccess {
        case (Some(game), Some(user)) => replyTo ! V.setup.challengeNotification(game, user)
      }

    case lila.hub.actorApi.RemindDeployPre  => sender ! V.notification.deploy("pre")
    case lila.hub.actorApi.RemindDeployPost => sender ! V.notification.deploy("post")

    case lila.tournament.actorApi.TournamentTable(tours) =>
      sender ! V.tournament.enterable(tours)

    case lila.puzzle.RenderDaily(puzzle, fen, lastMove) =>
      sender ! V.puzzle.daily(puzzle, fen, lastMove)

    case lila.tv.StreamsOnAir(streams) => sender ! V.tv.streamsOnAir(streams)
  }
}
