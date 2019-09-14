package lila.lobby

import scala.concurrent.duration._

import akka.actor.ActorSystem
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.game.{ Game, AnonCookie }
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.lobby._
import lila.hub.actorApi.timeline._
import lila.socket.{ Socket, SocketTrouper, LoneSocket }
import Socket.{ Sri, Sris }

private[lobby] final class LobbySocket(
    system: ActorSystem,
    sriTtl: FiniteDuration
) extends SocketTrouper[LobbySocketMember](system, sriTtl) with LoneSocket {

  def monitoringName = "lobby"
  def broomFrequency = 4073 millis

  system.lilaBus.subscribe(this, 'changeFeaturedGame, 'streams, 'poolGame, 'lobbySocket)
  system.scheduler.scheduleOnce(5 seconds)(this ! SendHookRemovals)
  system.scheduler.schedule(1 minute, 1 minute)(this ! Cleanup)

  private var idleSris = collection.mutable.Set[String]()

  private var hookSubscriberSris = collection.mutable.Set[String]()

  private var removedHookIds = ""

  def receiveSpecific = {

    case GetSrisP(promise) =>
      promise success Sris(members.keySet.map(Sri.apply)(scala.collection.breakOut))
      lila.mon.lobby.socket.idle(idleSris.size)
      lila.mon.lobby.socket.hookSubscribers(hookSubscriberSris.size)

    case Cleanup =>
      idleSris retain members.contains
      hookSubscriberSris retain members.contains

    case Join(sri, user, blocks, promise) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = LobbySocketMember(channel, user, blocks, sri)
      addMember(sri, member)
      promise success Connected(enumerator, member)

    case JoinRemote(member) =>
      members += (member.sri.value -> member)
    case LeaveRemote(sri) => quitRemote(sri)
    case LeaveRemotes(sris) => sris foreach quitRemote
    case LeaveAllRemote =>
      members.collect { case (_, m: LobbyRemoteSocketMember) => m.sri } foreach quitRemote

    case ReloadTournaments(html) => notifyAllActive(makeMessage("tournaments", html))

    case ReloadSimuls(html) => notifyAllActive(makeMessage("simuls", html))

    case ReloadTimelines(userIds) => userIds foreach { userId =>
      membersByUserId(userId) foreach (_ push messages.reloadTimeline)
    }

    case AddHook(hook) =>
      sendToActiveHookSubscribers(makeMessage("had", hook.render), member => Biter.showHookTo(hook, member))

    case AddSeek(_) => notifySeeks

    case RemoveHook(hookId) =>
      removedHookIds = s"$removedHookIds$hookId"

    case SendHookRemovals =>
      if (removedHookIds.nonEmpty) {
        sendToActiveHookSubscribers(makeMessage("hrm", removedHookIds))
        removedHookIds = ""
      }
      system.scheduler.scheduleOnce(1249 millis)(this ! SendHookRemovals)

    case RemoveSeek(_) => notifySeeks

    case JoinHook(sri, hook, game, creatorColor) =>
      withMember(hook.sri) { member =>
        lila.mon.lobby.hook.join()
        notifyPlayerStart(game, creatorColor)(member)
      }
      withMember(sri) { member =>
        lila.mon.lobby.hook.join()
        notifyPlayerStart(game, !creatorColor)(member)
      }

    case JoinSeek(userId, seek, game, creatorColor) =>
      membersByUserId(seek.user.id) foreach { member =>
        lila.mon.lobby.seek.join()
        notifyPlayerStart(game, creatorColor)(member)
      }
      membersByUserId(userId) foreach { member =>
        lila.mon.lobby.seek.join()
        notifyPlayerStart(game, !creatorColor)(member)
      }

    case pairing: lila.pool.PoolApi.Pairing =>
      def goPlayTheGame = redirectPlayers(pairing)
      goPlayTheGame // go play the game now
      system.scheduler.scheduleOnce(1 second)(goPlayTheGame) // I said go
      system.scheduler.scheduleOnce(3 second)(goPlayTheGame) // Darn it

    case HookIds(ids) =>
      sendToActiveHookSubscribers(makeMessage("hli", ids mkString ""))

    case lila.hub.actorApi.streamer.StreamsOnAir(html) => notifyAll(makeMessage("streams", html))

    case ChangeFeatured(_, msg) => notifyAllActive(msg)

    case SetIdle(sri, true) => idleSris += sri.value
    case SetIdle(sri, false) => idleSris -= sri.value

    case HookSub(member, false) => hookSubscriberSris -= member.sri.value
    case AllHooksFor(member, hooks) =>
      notifyMember("hooks", JsArray(hooks.map(_.render)))(member)
      hookSubscriberSris += member.sri.value

    // for LobbyRemoteSocket
    case GetRemoteMember(sri, promise) => promise success {
      members get sri.value collect { case m: LobbyRemoteSocketMember => m }
    }
  }

  private def sendToActiveHookSubscribers(msg: JsObject, condition: LobbySocketMember => Boolean = _ => true): Unit = {
    var remoteSris = List.empty[Sri]
    hookSubscriberSris diff idleSris foreach { sri =>
      members get sri filter condition foreach {
        case m: LobbyDirectSocketMember => m push msg
        case m => remoteSris = m.sri :: remoteSris
      }
    }
    system.lilaBus.publish(LobbySocketTellSris(remoteSris, msg), 'lobbySocketTell)
  }

  private def quitRemote(sri: Sri): Unit = {
    members -= sri.value
    afterQuit(sri)
  }

  // don't broom out remote socket members
  // since we don't get their pings and don't set them alive
  override protected def broom: Unit =
    members.foreachValue {
      case m @ LobbyDirectSocketMember(_, _, sri) if !aliveSris.get(sri.value) => eject(sri, m)
      case _ =>
    }

  private def redirectPlayers(p: lila.pool.PoolApi.Pairing) = {
    withMember(p.whiteSri)(notifyPlayerStart(p.game, chess.White))
    withMember(p.blackSri)(notifyPlayerStart(p.game, chess.Black))
  }

  private def notifyPlayerStart(game: Game, color: chess.Color) =
    notifyMember("redirect", Json.obj(
      "id" -> (game fullIdOf color),
      "url" -> playerUrl(game fullIdOf color)
    ).add("cookie" -> AnonCookie.json(game, color))) _

  // group remote socket messages!
  override protected def notifyAll(msg: JsObject): Unit = {
    members.foreachValue {
      case m: LobbyDirectSocketMember => m push msg
      case _ =>
    }
    system.lilaBus.publish(LobbySocketTellAll(msg), 'lobbySocketTell)
  }

  // TODO let lila-websocket know about active members
  private def notifyAllActive(msg: JsObject) = {
    members.foreach {
      case (sri, m: LobbyDirectSocketMember) if !idleSris(sri) => m push msg
      case _ =>
    }
    system.lilaBus.publish(LobbySocketTellActive(msg), 'lobbySocketTell)
  }

  override protected def afterQuit(sri: Sri, member: LobbySocketMember): Unit = afterQuit(sri)

  private def afterQuit(sri: Sri): Unit = {
    idleSris -= sri.value
    hookSubscriberSris -= sri.value
  }

  private def playerUrl(fullId: String) = s"/$fullId"

  private def notifySeeks = notifyAllActive(messages.reloadSeeks)

  private object messages {
    val reloadSeeks = makeMessage("reload_seeks")
    val reloadTimeline = makeMessage("reload_timeline")
  }

  private case object Cleanup
}
