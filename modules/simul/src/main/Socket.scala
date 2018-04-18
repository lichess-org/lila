package lila.simul

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }

private[simul] final class Socket(
    simulId: String,
    val history: History[Messadata],
    getSimul: Simul.ID => Fu[Option[Simul]],
    jsonView: JsonView,
    lightUser: lila.common.LightUser.Getter,
    uidTimeout: Duration,
    socketTimeout: Duration
) extends SocketActor[Member](uidTimeout) with Historical[Member, Messadata] {

  override def preStart(): Unit = {
    super.preStart()
    lilaBus.subscribe(self, Symbol(s"chat:$simulId"))
  }

  override def postStop(): Unit = {
    super.postStop()
    lilaBus.unsubscribe(self)
  }

  private val timeBomb = new TimeBomb(socketTimeout)

  private var delayedCrowdNotification = false

  private def redirectPlayer(game: lila.game.Game, colorOption: Option[chess.Color]): Unit = {
    colorOption foreach { color =>
      val player = game player color
      player.userId foreach { userId =>
        membersByUserId(userId) foreach { member =>
          notifyMember("redirect", game fullIdOf player.color)(member)
        }
      }
    }
  }

  def receiveSpecific = ({

    case StartGame(game, hostId) => redirectPlayer(game, game.playerByUserId(hostId) map (!_.color))

    case StartSimul(firstGame, hostId) => redirectPlayer(firstGame, firstGame.playerByUserId(hostId) map (_.color))

    case HostIsOn(gameId) => notifyVersion("hostGame", gameId, Messadata())

    case Reload =>
      getSimul(simulId) foreach {
        _ foreach { simul =>
          jsonView(simul) foreach { obj =>
            notifyVersion("reload", obj, Messadata())
          }
        }
      }

    case Aborted => notifyVersion("aborted", Json.obj(), Messadata())

    case Ping(uid, Some(v), c) => {
      ping(uid, c)
      timeBomb.delay
      withMember(uid) { m =>
        history.since(v).fold(resync(m))(_ foreach sendMessage(m))
      }
    }

    case Broom => {
      broom
      if (timeBomb.boom) self ! PoisonPill
    }

    case GetVersion => sender ! history.version

    case Socket.GetUserIds => sender ! members.values.flatMap(_.userId)

    case Join(uid, user) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid.value, member)
      notifyCrowd
      sender ! Connected(enumerator, member)

    case Quit(uid) =>
      quit(uid)
      notifyCrowd

    case NotifyCrowd =>
      delayedCrowdNotification = false
      showSpectators(lightUser)(members.values) foreach { notifyAll("crowd", _) }

  }: Actor.Receive) orElse lila.chat.Socket.out(
    send = (t, d, trollish) => notifyVersion(t, d, Messadata(trollish))
  )

  def notifyCrowd: Unit = {
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
