package lila.app
package actor

import akka.actor._
import play.api.templates.Html

import views.{ html ⇒ V }

private[app] final class Renderer extends Actor {

  def receive = {

    case lila.game.actorApi.RenderFeaturedJs(game) ⇒
      sender ! V.game.featuredJs(game)

    case lila.notification.actorApi.RenderNotification(id, from, body) ⇒
      sender ! V.notification.view(id, from)(Html(body))

    case lila.tournament.actorApi.RemindTournament(tournament) ⇒
      sender ! V.tournament.reminder(tournament)

    case lila.hub.actorApi.setup.RemindChallenge(gameId, from, _) ⇒
      sender ! V.setup.challengeNotification(gameId, from)

    case lila.hub.actorApi.RemindDeployPre ⇒ sender ! V.notification.deploy("pre")
    case lila.hub.actorApi.RemindDeployPost ⇒ sender ! V.notification.deploy("post")

    case lila.tournament.actorApi.TournamentTable(tours) ⇒
      sender ! V.tournament.createdTable(tours)

    case entry: lila.timeline.GameEntry ⇒
      sender ! V.timeline.gameEntry(entry)
  }
}
