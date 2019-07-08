package lila.simul

import akka.actor.ActorSystem
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
import lila.hub.Trouper
import lila.socket.{ SocketTrouper, History, Historical }
import lila.chat.Chat

private[simul] final class Socket(
    system: ActorSystem,
    simulId: String,
    protected val history: History[Messadata],
    getSimul: Simul.ID => Fu[Option[Simul]],
    jsonView: JsonView,
    lightUser: lila.common.LightUser.Getter,
    uidTtl: Duration,
    keepMeAlive: () => Unit
) extends SocketTrouper[Member](system, uidTtl) with Historical[Member, Messadata] {

  lilaBus.subscribe(this, chatClassifier)

  override def stop(): Unit = {
    super.stop()
    lilaBus.unsubscribe(this, chatClassifier)
  }

  private def chatClassifier = Chat classify Chat.Id(simulId)

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
          jsonView(simul, none) foreach { obj =>
            notifyVersion("reload", obj, Messadata())
          }
        }
      }

    case Aborted => notifyVersion("aborted", Json.obj(), Messadata())

    case lila.socket.Socket.GetVersion(promise) => promise success history.version

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

  }: Trouper.Receive) orElse lila.chat.Socket.out(
    send = (t, d, trollish) => notifyVersion(t, d, Messadata(trollish))
  )

  override protected def broom: Unit = {
    super.broom
    if (members.nonEmpty) keepMeAlive()
  }

  override protected def afterQuit(uid: lila.socket.Socket.Uid, member: Member) = notifyCrowd

  def notifyCrowd: Unit =
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      system.scheduler.scheduleOnce(500 millis)(this ! NotifyCrowd)
    }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    message.metadata.trollish && !member.troll
}
