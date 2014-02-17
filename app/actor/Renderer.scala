package lila.app
package actor

import akka.actor._
import play.api.templates.Html

import lila.game.GameRepo
import lila.user.UserRepo
import views.{ html => V }

private[app] final class Renderer extends Actor {

  def receive = {

    case lila.game.actorApi.RenderFeaturedJs(game) =>
      sender ! V.game.featuredJs(game)

    case lila.notification.actorApi.RenderNotification(id, from, body) =>
      sender ! V.notification.view(id, from)(Html(body))

    case lila.tournament.actorApi.RemindTournament(tournament) =>
      sender ! V.tournament.reminder(tournament)

    case lila.hub.actorApi.setup.RemindChallenge(gameId, from, _) => {
      val replyTo = sender
      (GameRepo game gameId) zip (UserRepo named from) foreach {
        case (Some(game), Some(user)) => replyTo ! V.setup.challengeNotification(game, user)
        case x                        => logwarn(s"remind challenge $x")
      }
    }

    case lila.hub.actorApi.RemindDeployPre  => sender ! V.notification.deploy("pre")
    case lila.hub.actorApi.RemindDeployPost => sender ! V.notification.deploy("post")

    case lila.tournament.actorApi.TournamentTable(tours) =>
      sender ! V.tournament.createdTable(tours)
  }
}
