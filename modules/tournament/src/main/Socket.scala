package lila.tournament

import scala.concurrent.duration.Duration

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.hub.TimeBomb
import lila.memo.ExpireSetMemo
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.socket.{ SocketActor, History, Historical }

private[tournament] final class Socket(
    tournamentId: String,
    val history: History,
    getUsername: String ⇒ Fu[Option[String]],
    uidTimeout: Duration,
    socketTimeout: Duration) extends SocketActor[Member](uidTimeout) with Historical[Member] {

  val joiningMemo = new ExpireSetMemo(uidTimeout)

  private val timeBomb = new TimeBomb(socketTimeout)

  def receiveSpecific = {

    case StartGame(game) ⇒ game.players foreach { player ⇒
      for {
        userId ← player.userId
        member ← memberByUserId(userId)
      } {
        notifyMember("redirect", game fullIdOf player.color)(member)
        notifyReload
      }
    }

    case Reload     ⇒ notifyReload

    case Start      ⇒ notifyVersion("start", JsNull)

    case ReloadPage ⇒ notifyVersion("reloadPage", JsNull)

    case PingVersion(uid, v) ⇒ {
      ping(uid)
      timeBomb.delay
      withMember(uid) { m ⇒
        history.since(v).fold(resync(m))(_ foreach sendMessage(m))
      }
    }

    case Broom ⇒ {
      broom
      if (timeBomb.boom) self ! PoisonPill
    }

    case lila.chat.actorApi.ChatLine(_, line) ⇒ line match {
      case line: lila.chat.UserLine ⇒
        notifyVersionTrollable("message", lila.chat.Line toJson line, troll = line.troll)
      case _ ⇒
    }

    case GetVersion ⇒ sender ! history.version

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

    case Joining(userId) ⇒ joiningMemo put userId
  }

  override def userIds = (super.userIds ++ joiningMemo.keys).toList.distinct

  def notifyCrowd {
    members.values.map(_.userId).toList.partition(_.isDefined) match {
      case (users, anons) ⇒
        (users.flatten.distinct.sorted map getUsername).sequenceFu foreach { userList ⇒
          notifyVersion("crowd", showSpectators(userList.flatten, anons.size))
        }
    }
  }

  def notifyReload {
    notifyVersion("reload", JsNull)
  }

  def notifyVersionTrollable[A: Writes](t: String, data: A, troll: Boolean) {
    val vmsg = history += History.Message(makeMessage(t, data), troll)
    members.values.foreach(sendMessage(vmsg))
  }
}
