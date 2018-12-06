package lila.tournament

import akka.actor._
import akka.pattern.pipe
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.collection.breakOut
import scala.concurrent.duration._

import actorApi._
import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketTrouper, History, Historical }

private[tournament] final class Socket(
    val system: ActorSystem,
    tournamentId: String,
    val history: History[Messadata],
    jsonView: JsonView,
    lightUser: lila.common.LightUser.Getter,
    val uidTtl: Duration
) extends SocketTrouper[Member](uidTtl) with Historical[Member, Messadata] {

  private var delayedCrowdNotification = false
  private var delayedReloadNotification = false

  private var clock = none[chess.Clock.Config]

  private var waitingUsers = WaitingUsers.empty

  override def start(): Unit = {
    super.start()
    lilaBus.subscribe(this, Symbol(s"chat:$tournamentId"))
    TournamentRepo byId tournamentId foreach { t =>
      this ! SetTournament(t)
    }
  }

  override def stop(): Unit = {
    super.stop()
    lilaBus.unsubscribe(this)
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

    case GetWaitingUsersP(promise) =>
      promise success waitingUsers.update(members.values.flatMap(_.userId)(breakOut), clock)

    case Ping(uid, vOpt, lt) =>
      ping(uid, lt)
      pushEventsSinceForMobileBC(vOpt, uid)

    case lila.socket.Socket.GetVersionP(promise) => promise success history.version

    case JoinP(uid, user, version, promise) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      notifyCrowd
      promise success Connected(
        prependEventsSince(version, enumerator, member),
        member
      )

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

  def notifyCrowd: Unit =
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      system.scheduler.scheduleOnce(1 second)(this ! NotifyCrowd)
    }

  def notifyReload: Unit =
    if (!delayedReloadNotification) {
      delayedReloadNotification = true
      // keep the delay low for immediate response to join/withdraw,
      // but still debounce to avoid tourney start message rush
      system.scheduler.scheduleOnce(1 second)(this ! NotifyReload)
    }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    message.metadata.trollish && !member.troll
}
