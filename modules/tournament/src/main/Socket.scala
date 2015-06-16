package lila.tournament

import akka.actor._
import akka.pattern.pipe
import org.joda.time.DateTime
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.common.LightUser
import lila.hub.actorApi.WithUserIds
import lila.hub.TimeBomb
import lila.memo.ExpireSetMemo
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }

private[tournament] final class Socket(
    tournamentId: String,
    val history: History[Messadata],
    jsonView: JsonView,
    lightUser: String => Option[LightUser],
    uidTimeout: Duration,
    socketTimeout: Duration) extends SocketActor[Member](uidTimeout) with Historical[Member, Messadata] {

  private val joiningMemo = new ExpireSetMemo(uidTimeout)

  private val timeBomb = new TimeBomb(socketTimeout)

  private var delayedCrowdNotification = false
  private var delayedReloadNotification = false

  private var clock = none[chess.Clock]

  override def preStart() {
    super.preStart()
    TournamentRepo byId tournamentId map SetTournament.apply pipeTo self
  }

  def receiveSpecific = {

    case SetTournament(Some(tour)) =>
      clock = tour.clock.chessClock.some

    case StartGame(game) =>
      game.players foreach { player =>
        player.userId foreach { userId =>
          membersByUserId(userId) foreach { member =>
            notifyMember("redirect", game fullIdOf player.color)(member)
          }
        }
      }
      notifyReload

    case Reload        => notifyReload

    case GetAllUserIds => sender ! AllUserIds(all = userIds, waiting = waitingUserIds)

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
        notifyVersion("message", lila.chat.Line toJson line, Messadata(line.troll))
      case _ =>
    }

    case GetVersion => sender ! history.version

    case Join(uid, user, version) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      notifyCrowd
      sender ! Connected(enumerator, member)

    case Quit(uid) =>
      quit(uid)
      notifyCrowd

    case Joining(userId) => joiningMemo put userId

    case NotifyCrowd =>
      delayedCrowdNotification = false
      val (anons, users) = members.values.map(_.userId flatMap lightUser).foldLeft(0 -> List[LightUser]()) {
        case ((anons, users), Some(user)) => anons -> (user :: users)
        case ((anons, users), None)       => (anons + 1) -> users
      }
      notifyAll("crowd", showSpectators(users, anons))

    case NotifyReload =>
      delayedReloadNotification = false
      notifyAll("reload")
  }

  private var waitingUsers = Map[String, DateTime]()

  // users that have been here for some time
  def waitingUserIds: List[String] = {
    val us = userIds
    val date = DateTime.now
    waitingUsers = waitingUsers.filterKeys { u =>
      us contains u
    }.++ {
      us.filterNot(waitingUsers.contains).map { _ -> date }
    }
    // 1+0  5  -> 10
    // 3+0  9  -> 14
    // 5+0  17 -> 22
    // 10+0 32 -> 35
    val waitSeconds = ((clock.fold(60)(_.estimateTotalTime) / 15) + 2) min 35 max 10
    val since = date minusSeconds waitSeconds
    waitingUsers.collect {
      case (u, d) if d.isBefore(since) => u
    }.toList
  }

  def notifyCrowd {
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      context.system.scheduler.scheduleOnce(500 millis, self, NotifyCrowd)
    }
  }

  def notifyReload {
    if (!delayedReloadNotification) {
      delayedReloadNotification = true
      // keep the delay low for immediate response to join/withdraw,
      // but still debounce to avoid tourney start message rush
      context.system.scheduler.scheduleOnce(200 millis, self, NotifyReload)
    }
  }

  override def userIds = (super.userIds ++ joiningMemo.keys).toList.distinct

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    message.metadata.trollish && !member.troll
}
