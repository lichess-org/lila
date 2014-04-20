package lila.app
package actor

import lila.hub.actorApi.router._
import lila.i18n.I18nDomain

import akka.actor._
import akka.pattern.{ ask, pipe }
import controllers.{ routes => R }

// returns String urls, not Call objects
private[app] final class Router(
    baseUrl: String,
    protocol: String,
    domain: String) extends Actor {

  import makeTimeout.large

  def receive = {

    case Abs(route) => self ? route map {
      case route: String => baseUrl + route
    } pipeTo sender

    case Nolang(route) => self ? route map {
      case route: String => noLangBaseUrl + route
    } pipeTo sender

    case Homepage               => sender ! R.Lobby.home().url
    case TeamShow(id)           => sender ! R.Team.show(id).url
    case User(username)         => sender ! R.User.show(username).url
    case Player(fullId)         => sender ! R.Round.player(fullId).url
    case Watcher(gameId, color) => sender ! R.Round.watcher(gameId, color).url
    case Pgn(gameId)            => sender ! R.Analyse.pgn(gameId).url
    case Tourney(tourId)        => sender ! R.Tournament.show(tourId).url
    case Puzzle(id)             => sender ! R.Puzzle.show(id).url

    case msg                    => sender ! Status.Failure(new Exception(s"No route for $msg"))
  }

  private lazy val noLangBaseUrl = protocol + I18nDomain(domain).commonDomain
}
