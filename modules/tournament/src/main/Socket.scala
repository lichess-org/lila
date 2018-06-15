package lila.tournament

import akka.actor._
import akka.pattern.pipe
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._
import scala.collection.breakOut

import actorApi._
import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }

private[tournament] final class Socket(
    tournamentId: String,
    val history: History[Messadata],
    jsonView: JsonView,
    lightUser: lila.common.LightUser.Getter,
    uidTimeout: Duration,
    socketTimeout: Duration
) extends SocketActor[Member](uidTimeout) with Historical[Member, Messadata] {

  private val timeBomb = new TimeBomb(socketTimeout)

  private var delayedCrowdNotification = false
  private var delayedReloadNotification = false

  private var clock = none[chess.Clock.Config]

  private var waitingUsers = WaitingUsers.empty

  override def preStart(): Unit = {
    super.preStart()
    lilaBus.subscribe(self, Symbol(s"chat:$tournamentId"))
    TournamentRepo byId tournamentId map SetTournament.apply pipeTo self
  }

  override def postStop(): Unit = {
    super.postStop()
    lilaBus.unsubscribe(self)
  }

  def receiveSpecific = ({

    case SetTournament(Some(tour)) => clock = tour.clock.some

    case StartGame(game) =>
      game.players foreach { player =>
        player.userId foreach { userId =>
          firstMemberByUserId(userId) foreach { member =>
            notifyMember("redirect", game fullIdOf player.color)(member)
          }
        }
      }
      notifyReload

    case Reload => notifyReload

    case GetWaitingUsers =>
      waitingUsers = waitingUsers.update(members.values.flatMap(_.userId)(breakOut), clock)
      sender ! waitingUsers

    case Ping(uid, Some(v), lt) => {
      ping(uid, lt)
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
      showSpectators(lightUser)(members.values) foreach {
        notifyAll("crowd", _)
      }

    case NotifyReload =>
      delayedReloadNotification = false
      notifyAll("reload")

  }: Actor.Receive) orElse lila.chat.Socket.out(
    send = (t, d, trollish) => notifyVersion(t, d, Messadata(trollish))
  )

  def notifyCrowd: Unit = {
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      context.system.scheduler.scheduleOnce(1000 millis, self, NotifyCrowd)
    }
  }

  def notifyReload: Unit = {
    if (!delayedReloadNotification) {
      delayedReloadNotification = true
      // keep the delay low for immediate response to join/withdraw,
      // but still debounce to avoid tourney start message rush
      context.system.scheduler.scheduleOnce(700 millis, self, NotifyReload)
    }
  }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    message.metadata.trollish && !member.troll
}
