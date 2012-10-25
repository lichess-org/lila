package lila
package tournament

import akka.actor._
import play.api.libs.concurrent._
import play.api.Play.current

private[tournament] final class Register extends Actor {

  var userTournaments = Map[String, Tournament]()

  def receive = {

    case SetTournaments(tours) ⇒ {
      userTournaments = tours.foldLeft(Map[String, Tournament]()) {
        case (ts, tour) ⇒ ts ++ tour.userIds.map(x ⇒ x -> tour)
      }
      print(userTournaments)
    }

    case GetUserTournament(userId) ⇒ sender ! (userTournaments get userId)
  }
}
