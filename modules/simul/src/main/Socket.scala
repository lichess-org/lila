package lila.simul

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.common.LightUser
import lila.hub.actorApi.round.MoveEvent
import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }

private[simul] final class Socket(
    simulId: String,
    val history: History[Messadata],
    getSimul: Simul.ID => Fu[Option[Simul]],
    jsonView: JsonView,
    lightUser: String => Option[LightUser],
    uidTimeout: Duration,
    socketTimeout: Duration) extends SocketActor[Member](uidTimeout) with Historical[Member, Messadata] {

  override def preStart() {
    lilaBus.subscribe(self, Symbol(s"chat-$simulId"))
  }

  override def postStop() {
    super.postStop()
    lilaBus.unsubscribe(self)
  }

  private val timeBomb = new TimeBomb(socketTimeout)

  private var delayedCrowdNotification = false

  private def redirectPlayer(game: lila.game.Game, colorOption: Option[chess.Color]) {
    colorOption foreach { color =>
      val player = game player color
      player.userId foreach { userId =>
        membersByUserId(userId) foreach { member =>
          notifyMember("redirect", game fullIdOf player.color)(member)
        }
      }
    }
  }

  def receiveSpecific = {

    case StartGame(game, hostId)       => redirectPlayer(game, game.playerByUserId(hostId) map (!_.color))

    case StartSimul(firstGame, hostId) => redirectPlayer(firstGame, firstGame.playerByUserId(hostId) map (_.color))

    case HostIsOn(gameId)              => notifyVersion("hostGame", gameId, Messadata())

    case Reload =>
      getSimul(simulId) foreach {
        _ foreach { simul =>
          jsonView(simul) foreach { obj =>
            notifyVersion("reload", obj, Messadata())
          }
        }
      }

    case Aborted => notifyVersion("aborted", Json.obj(), Messadata())

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
        notifyVersion("message", lila.chat.JsonView(line), Messadata(line.troll))
      case _ =>
    }

    case lila.chat.actorApi.MarkDeleted(username) =>
      notifyVersion("chat_mark_deleted", username, Messadata())

    case GetVersion        => sender ! history.version

    case Socket.GetUserIds => sender ! userIds

    case Join(uid, user) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      notifyCrowd
      sender ! Connected(enumerator, member)

    case Quit(uid) =>
      quit(uid)
      notifyCrowd

    case NotifyCrowd =>
      delayedCrowdNotification = false
      notifyAll("crowd", showSpectators(lightUser)(members.values))
  }

  def notifyCrowd {
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      context.system.scheduler.scheduleOnce(500 millis, self, NotifyCrowd)
    }
  }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    message.metadata.trollish && !member.troll
}

case object Socket {
  case object GetUserIds
}
