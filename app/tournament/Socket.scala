package lila
package tournament

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import akka.dispatch.Await
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.Play.current
import scalaz.effects._

import user.User
import game.DbGame
import socket.{ PingVersion, Quit, LiveGames }
import socket.Util.connectionFail
import security.Flood
import implicits.RichJs._

final class Socket(
    getTournament: String ⇒ IO[Option[Tournament]],
    hubMaster: ActorRef,
    messenger: Messenger,
    flood: Flood) {

  private val timeoutDuration = 1 second
  implicit private val timeout = Timeout(timeoutDuration)

  def start(tournamentId: String) = io {
    hubMaster ! Forward(tournamentId, Start)
  }
 
  def reload(tournamentId: String) = io {
    hubMaster ! Forward(tournamentId, Reload)
  }

  def reloadPage(tournamentId: String) = io {
    hubMaster ! Forward(tournamentId, ReloadPage)
  }

  def notifyPairing(game: DbGame) = io {
    game.tournamentId foreach { tid ⇒
      hubMaster ! Forward(tid, StartGame(game))
    }
  }

  def notifyJoining(tournamentId: String, userId: String) = io {
    hubMaster ! Forward(tournamentId, Joining(userId))
  }

  def join(
    tournamentId: String,
    version: Option[Int],
    uid: Option[String],
    user: Option[User]): IO[SocketPromise] =
    getTournament(tournamentId) map { tourOption ⇒
      ((tourOption |@| uid |@| version) apply {
        (tour: Tournament, uid: String, version: Int) ⇒
          (for {
            hub ← hubMaster ? GetHub(tournamentId) mapTo manifest[ActorRef]
            socket ← hub ? Join(
              uid = uid,
              user = user,
              version = version
            ) map {
                case Connected(enumerator, member) ⇒ (
                  Iteratee.foreach[JsValue](
                    controller(hub, uid, member, tournamentId)
                  ) mapDone { _ ⇒
                      hub ! Quit(uid)
                    },
                    enumerator)
              }
          } yield socket).asPromise: SocketPromise
      }) | connectionFail
    }

  private def controller(
    hub: ActorRef,
    uid: String,
    member: Member,
    tournamentId: String): JsValue ⇒ Unit = e ⇒ e str "t" match {
    case Some("p") ⇒ e int "v" foreach { v ⇒
      hub ! PingVersion(uid, v)
    }
    case Some("liveGames") ⇒ e str "d" foreach { ids ⇒
      hub ! LiveGames(uid, ids.split(' ').toList)
    }
    case Some("talk") ⇒ for {
      username ← member.username
      txt ← e str "d"
      if flood.allowMessage(uid, txt)
    } hub ! Talk(username, txt)
    case _ ⇒
  }

  def blockingVersion(tournamentId: String): Int = Await.result(
    hubMaster ? GetTournamentVersion(tournamentId) mapTo manifest[Int],
    timeoutDuration)
}
