package lidraughts.simul

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lidraughts.hub.TimeBomb
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.{ SocketActor, History, Historical }

private[simul] final class Socket(
    simulId: String,
    val history: History[Messadata],
    getSimul: Simul.ID => Fu[Option[Simul]],
    jsonView: JsonView,
    lightUser: lidraughts.common.LightUser.Getter,
    uidTimeout: Duration,
    socketTimeout: Duration
) extends SocketActor[Member](uidTimeout) with Historical[Member, Messadata] {

  override def preStart(): Unit = {
    super.preStart()
    lidraughtsBus.subscribe(self, Symbol(s"chat:$simulId"))
  }

  override def postStop(): Unit = {
    super.postStop()
    lidraughtsBus.unsubscribe(self)
  }

  private val timeBomb = new TimeBomb(socketTimeout)

  private var delayedCrowdNotification = false

  private def redirectPlayer(game: lidraughts.game.Game, colorOption: Option[draughts.Color]): Unit = {
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
          jsonView(simul, false, none) foreach { obj =>
            notifyVersion("reload", obj, Messadata())
          }
        }
      }

    case ReloadEval(gameId, json) =>
      getSimul(simulId) foreach {
        _ foreach { simul =>
          jsonView.evalWithGame(simul, gameId, json) foreach { obj =>
            notifyVersionIf("ceval", obj, Messadata())(m => simul.canHaveCeval(m.userId))
          }
        }
      }

    case Aborted => notifyVersion("aborted", Json.obj(), Messadata())

    case Ping(uid, vOpt, c) => {
      ping(uid, c)
      timeBomb.delay

      // Mobile backwards compat
      vOpt foreach { v =>
        withMember(uid) { m =>
          history.since(v).fold(resync(m))(_ foreach sendMessage(m))
        }
      }
    }

    case Broom => {
      broom
      if (timeBomb.boom) self ! PoisonPill
    }

    case lidraughts.socket.Socket.GetVersion => sender ! history.version

    case Socket.GetUserIds => sender ! members.values.flatMap(_.userId)

    case Join(uid, user, version) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      notifyCrowd

      val msgs: List[JsValue] = version
        .fold(history.getRecent(5).some)(history.since)
        .fold(List(resyncMessage))(_ map filteredMessage(member))

      sender ! Connected(
        lidraughts.common.Iteratee.prepend(msgs, enumerator),
        member
      )

    case Quit(uid) =>
      quit(uid)
      notifyCrowd

    case NotifyCrowd =>
      delayedCrowdNotification = false
      showSpectators(lightUser)(members.values) foreach { notifyAll("crowd", _) }

  }: Actor.Receive) orElse lidraughts.chat.Socket.out(
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
