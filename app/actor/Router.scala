package lila.app
package actor

import lila.hub.actorApi.router._
import lila.i18n.I18nDomain

import controllers.{ routes ⇒ R }
import akka.actor._
import akka.pattern.{ ask, pipe }

private[app] final class Router(
  baseUrl: String,
  protocol: String,
  domain: String) extends Actor {

  import makeTimeout.large

  def receive = {

    case Abs(route) ⇒ self ? route map { 
      case route: String ⇒ baseUrl + route
    } pipeTo sender

    case Nolang(route) ⇒ self ? route map { 
      case route: String ⇒ noLangBaseUrl + route
    } pipeTo sender

    case TeamShow(id)   ⇒ sender ! R.Team.show(id)

    case Player(fullId) ⇒ sender ! R.Round.player(fullId)
  }

  private lazy val noLangBaseUrl = protocol + I18nDomain(domain).commonDomain
}

