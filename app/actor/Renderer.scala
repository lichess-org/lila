package lila.app
package actor

import akka.actor._

import play.api.templates.Html
import views.{ html ⇒ V }

private[app] final class Renderer extends Actor {

  def receive = {

    case lila.game.actorApi.RenderFeaturedJs(game) ⇒
      sender ! V.game.featuredJsNoCtx(game)

    case lila.notification.actorApi.RenderNotification(id, from, body) ⇒
      sender ! V.notification.view(id, from)(Html(body))

    case lila.tournament.actorApi.RemindTournament(tournament) ⇒
      sender ! V.tournament.reminder(tournament)

    case lila.tournament.actorApi.TournamentTable(tours) ⇒
      sender ! V.tournament.createdTable(tours)
  }
}
