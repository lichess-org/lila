package lidraughts.simul

import akka.actor.ActorSystem
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lidraughts.chat.Chat
import lidraughts.hub.{ TimeBomb, Trouper }
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.{ SocketTrouper, History, Historical }

private[simul] final class Socket(
    system: ActorSystem,
    simulId: String,
    protected val history: History[Messadata],
    getSimul: Simul.ID => Fu[Option[Simul]],
    jsonView: JsonView,
    lightUser: lidraughts.common.LightUser.Getter,
    uidTtl: Duration,
    keepMeAlive: () => Unit
) extends SocketTrouper[Member](system, uidTtl) with Historical[Member, Messadata] {

  lidraughtsBus.subscribe(this, chatClassifier)

  override def stop(): Unit = {
    super.stop()
    lidraughtsBus.unsubscribe(this, chatClassifier)
  }

  private def chatClassifier = Chat classify Chat.Id(simulId)

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

    case HostIsOn(gameId) => notifyVersion("hostGame", gameId, noMessadata)

    case Reload =>
      getSimul(simulId) foreach {
        _ foreach { simul =>
          jsonView(simul, false, none) foreach { obj =>
            notifyVersion("reload", obj, noMessadata)
          }
        }
      }

    case ReloadEval(gameId, json) =>
      getSimul(simulId) foreach {
        _ foreach { simul =>
          jsonView.evalWithGame(simul, gameId, json) foreach { obj =>
            notifyVersionIf("ceval", obj, noMessadata)(m => simul.canHaveCeval(m.userId))
          }
        }
      }

    case Aborted => notifyVersion("aborted", Json.obj(), noMessadata)

    case lidraughts.socket.Socket.GetVersion(promise) => promise success history.version

    case GetUserIdsP(promise) => promise success members.values.flatMap(_.userId)

    case Join(uid, user, version, promise) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      notifyCrowd
      promise success Connected(
        prependEventsSince(version, enumerator, member),
        member
      )

    case NotifyCrowd =>
      delayedCrowdNotification = false
      showSpectators(lightUser)(members.values) foreach { notifyAll("crowd", _) }

  }: Trouper.Receive) orElse lidraughts.chat.Socket.out(
    send = (t, d, trollish) => notifyVersion(t, d, Messadata(trollish))
  )

  override protected def broom: Unit = {
    super.broom
    if (members.nonEmpty) keepMeAlive()
  }

  override protected def afterQuit(uid: lidraughts.socket.Socket.Uid, member: Member) = notifyCrowd

  def notifyCrowd: Unit =
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      system.scheduler.scheduleOnce(500 millis)(this ! NotifyCrowd)
    }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    message.metadata.trollish && !member.troll

  private val noMessadata = Messadata()
}
