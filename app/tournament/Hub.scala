package lila
package tournament

import game.DbGame
import socket._

import akka.actor._
import akka.util.duration._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.Play.current
import scalaz.effects._

final class Hub(
    tournamentId: String,
    val history: History,
    messenger: Messenger,
    uidTimeout: Int,
    hubTimeout: Int) extends HubActor[Member](uidTimeout) with Historical[Member] {

  var lastPingTime = nowMillis

  def receiveSpecific = {

    case StartGame(game: DbGame) ⇒ game.players foreach { player ⇒
      for {
        userId ← player.userId
        member ← memberByUserId(userId)
      } notifyMember("redirect", JsString(game fullIdOf player.color))(member)
    }

    case PingVersion(uid, v) ⇒ {
      ping(uid)
      lastPingTime = nowMillis
      withMember(uid) { m ⇒
        history.since(v).fold(_ foreach m.channel.push, resync(m))
      }
    }

    case Broom ⇒ {
      broom()
      if (lastPingTime < (nowMillis - hubTimeout)) {
        context.parent ! CloseTournament(tournamentId)
      }
    }

    case Talk(u, txt) ⇒
      messenger.userMessage(tournamentId, u, txt).unsafePerformIO foreach { message ⇒
        notifyVersion("talk", JsString(message.render))
      }

    case ReloadUserList          ⇒ notifyVersion("users", JsNull)

    case GetTournamentVersion(_) ⇒ sender ! history.version

    case Join(uid, user, version) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      sender ! Connected(enumerator, member)
    }

    case Close ⇒ {
      members.values foreach { _.channel.end() }
      self ! PoisonPill
    }
  }
}
