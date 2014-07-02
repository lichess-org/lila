package lila.tournament

import scala.concurrent.duration.Duration

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.common.{ LightUser, Debouncer }
import lila.hub.TimeBomb
import lila.memo.ExpireSetMemo
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }

private[tournament] final class Socket(
    tournamentId: String,
    val history: History,
    lightUser: String => Option[LightUser],
    uidTimeout: Duration,
    socketTimeout: Duration) extends SocketActor[Member](uidTimeout) with Historical[Member] {

  val joiningMemo = new ExpireSetMemo(uidTimeout)

  private val timeBomb = new TimeBomb(socketTimeout)

  def receiveSpecific = {

    case StartGame(game) =>
      game.players foreach { player =>
        player.userId flatMap memberByUserId foreach { member =>
          notifyMember("redirect", game fullIdOf player.color)(member)
        }
      }
      reloadNotifier ! Debouncer.Nothing

    case Reload     => reloadNotifier ! Debouncer.Nothing

    case Start      => notifyVersion("start", JsNull)

    case ReloadPage => notifyVersion("reloadPage", JsNull)

    case PingVersion(uid, v) => {
      ping(uid)
      timeBomb.delay
      withMember(uid) { m =>
        history.since(v).fold(resync(m))(_ foreach sendMessage(m))
      }
    }

    case Broom => {
      broom
      if (timeBomb.boom) self ! PoisonPill
    }

    case lila.chat.actorApi.ChatLine(_, line) => line match {
      case line: lila.chat.UserLine =>
        notifyVersionTrollable("message", lila.chat.Line toJson line, troll = line.troll)
      case _ =>
    }

    case GetVersion => sender ! history.version

    case Join(uid, user, version) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      crowdNotifier ! members.values
      sender ! Connected(enumerator, member)

    case Quit(uid) =>
      quit(uid)
      crowdNotifier ! members.values

    case Joining(userId) => joiningMemo put userId
  }

  override def userIds = (super.userIds ++ joiningMemo.keys).toList.distinct

  val crowdNotifier =
    context.system.actorOf(Props(new Debouncer(1.seconds, (ms: Iterable[Member]) => {
      val (anons, users) = members.values.map(_.userId flatMap lightUser).foldLeft(0 -> List[LightUser]()) {
        case ((anons, users), Some(user)) => anons -> (user :: users)
        case ((anons, users), None)       => (anons + 1) -> users
      }
      notifyVersion("crowd", showSpectators(users, anons))
    })))

  val reloadNotifier =
    context.system.actorOf(Props(new Debouncer(1.seconds, (_: Debouncer.Nothing) => {
      notifyAll(makeMessage("reload"))
    })))

  def notifyVersionTrollable[A: Writes](t: String, data: A, troll: Boolean) {
    val vmsg = history.+=(makeMessage(t, data), troll)
    members.values.foreach(sendMessage(vmsg))
  }
}
