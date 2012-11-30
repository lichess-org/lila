package lila
package tournament

import game.DbGame
import socket._
import memo.BooleanExpiryMemo

import akka.actor._
import scala.concurrent.duration._
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

  val joiningMemo = new BooleanExpiryMemo(uidTimeout)

  var lastPingTime = nowMillis

  def receiveSpecific = {

    case StartGame(game: DbGame) ⇒ game.players foreach { player ⇒
      for {
        userId ← player.userId
        member ← memberByUserId(userId)
      } {
        notifyMember("redirect", JsString(game fullIdOf player.color))(member)
        notifyReload
      }
    }

    case Reload     ⇒ notifyReload

    case Start      ⇒ notifyVersion("start", JsNull)

    case ReloadPage ⇒ notifyVersion("reloadPage", JsNull)

    case PingVersion(uid, v) ⇒ {
      ping(uid)
      lastPingTime = nowMillis
      withMember(uid) { m ⇒
        history.since(v).fold(resync(m))(_ foreach m.channel.push)
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

    case GetTournamentVersion(_) ⇒ sender ! history.version

    case Join(uid, user, version) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      notifyCrowd
      sender ! Connected(enumerator, member)
    }

    case Quit(uid) ⇒ {
      quit(uid)
      notifyCrowd
    }

    case Close ⇒ {
      members.values foreach { _.channel.end() }
      self ! PoisonPill
    }

    case Joining(userId) ⇒ (joiningMemo put userId).unsafePerformIO
  }

  override def usernames = (super.usernames ++ joiningMemo.keys).toList.distinct

  def notifyCrowd {
    notifyVersion("crowd", JsArray({
      members.values
        .map(_.username)
        .toList.partition(_.isDefined) match {
          case (users, anons) ⇒ users.flatten.distinct |> { userList ⇒
            anons.size match {
              case 0 ⇒ userList
              case 1 ⇒ userList :+ "Anonymous"
              case x ⇒ userList :+ ("Anonymous (%d)" format x)
            }
          }
        }
    } map { JsString(_) }))
  }

  def notifyReload {
    notifyVersion("reload", JsNull)
  }
}
