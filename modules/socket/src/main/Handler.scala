package lila.socket

import akka.actor.ActorRef
import akka.pattern.ask
import ornicar.scalalib.Zero
import play.api.libs.iteratee.Iteratee
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import chess.Centis
import lila.common.ApiVersion
import lila.common.PimpedJson.centisReads
import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.Trouper
import lila.socket.Socket.makeMessage

object Handler {

  type Controller = PartialFunction[(String, JsObject), Unit]
  type Connection = (Controller, JsEnumerator, SocketMember)
  type ActorConnecter = PartialFunction[Any, Connection]
  type TrouperConnecter = PartialFunction[Any, Connection]

  private val AnaRateLimiter = new lila.memo.RateLimit[String](120, 30 seconds,
    name = "socket analysis move",
    key = "socket_analysis_move")

  def AnaRateLimit[A: Zero](sri: Socket.Sri, member: SocketMember)(op: => A) =
    AnaRateLimiter(sri.value, msg = s"user: ${member.userId | "anon"}")(op)

  type OnPing = (SocketTrouper[_], SocketMember, Socket.Sri, ApiVersion) => Unit

  val defaultOnPing: OnPing = (socket, member, sri, apiVersion) => {
    socket setAlive sri
    member push {
      if (apiVersion gte 4) Socket.emptyPong
      else Socket.initialPong
    }
  }

  def iteratee(
    hub: lila.hub.Env,
    controller: Controller,
    member: SocketMember,
    socket: SocketTrouper[_],
    sri: Socket.Sri,
    apiVersion: ApiVersion,
    onPing: OnPing = defaultOnPing
  ): JsIteratee = {
    val fullCtrl = controller orElse baseController(hub, socket, member, sri, apiVersion, onPing)
    Iteratee.foreach[JsValue] { v =>
      if (!socket.getIsAlive) {
        // this socket is dead, ignore message and tell client to reconnect to the new socket
        lila.mon.socket.deadMsg()
        member push SocketTrouper.resyncMessage
      } // process null ping immediately
      else if (v == JsNull) onPing(socket, member, sri, apiVersion)
      else for {
        obj <- v.asOpt[JsObject]
        t <- (obj \ "t").asOpt[String]
      } fullCtrl(t -> obj)
    }
      // Unfortunately this map function is only called
      // if the JS closes the socket with lichess.socket.disconnect()
      // but not if the tab is closed or browsed away!
      // Also if the client loses Internet connection,
      // this will only be called after Internet is restored,
      // and it can be called after a reconnection (using same sri) was performed,
      // effectively quitting the reconnected client.
      .map(_ => socket ! Quit(sri, member))
  }

  def recordUserLagFromPing(member: SocketMember, ping: JsObject) = for {
    user <- member.userId
    lag <- (ping \ "l").asOpt[Centis]
  } UserLagCache.put(user, lag)

  private def baseController(
    hub: lila.hub.Env,
    socket: SocketTrouper[_],
    member: SocketMember,
    sri: Socket.Sri,
    apiVersion: ApiVersion,
    onPing: OnPing
  ): Controller = {
    // latency ping, or BC mobile app ping
    case ("p", o) =>
      onPing(socket, member, sri, apiVersion)
      recordUserLagFromPing(member, o)
    case ("following_onlines", _) => member.userId foreach { u =>
      hub.relation ! ReloadOnlineFriends(u)
    }
    case ("startWatching", o) => o str "d" foreach { ids =>
      hub.bus.publish(StartWatching(sri, member, ids.split(' ').toSet), 'socketMoveBroadcast)
    }
    case ("moveLat", o) => hub.bus.publish(
      if (~(o boolean "d")) Channel.Sub(member) else Channel.UnSub(member),
      'roundMoveTimeChannel
    )
    case ("anaMove", o) => AnaRateLimit(sri, member) {
      AnaMove parse o foreach { anaMove =>
        member push {
          anaMove.branch match {
            case scalaz.Success(node) => makeMessage("node", anaMove json node)
            case scalaz.Failure(err) => makeMessage("stepFailure", err.toString)
          }
        }
      }
    }
    case ("anaDrop", o) => AnaRateLimit(sri, member) {
      AnaDrop parse o foreach { anaDrop =>
        anaDrop.branch match {
          case scalaz.Success(branch) =>
            member push makeMessage("node", anaDrop json branch)
          case scalaz.Failure(err) =>
            member push makeMessage("stepFailure", err.toString)
        }
      }
    }
    case ("anaDests", o) => AnaRateLimit(sri, member) {
      AnaDests parse o foreach { res =>
        member push makeMessage("dests", res.json)
      }
    }
    case ("opening", o) => AnaRateLimit(sri, member) {
      GetOpening(o) foreach { res =>
        member push makeMessage("opening", res)
      }
    }
    case ("notified", _) => member.userId foreach { userId =>
      hub.notification ! lila.hub.actorApi.notify.Notified(userId)
    }
    case _ => // logwarn("Unhandled msg: " + msg)
  }

}
