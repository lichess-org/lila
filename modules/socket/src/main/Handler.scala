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

  def AnaRateLimit[A: Zero](uid: Socket.Uid, member: SocketMember)(op: => A) =
    AnaRateLimiter(uid.value, msg = s"user: ${member.userId | "anon"}")(op)

  type OnPing = (SocketTrouper[_], SocketMember, Socket.Uid, ApiVersion) => Unit

  val defaultOnPing: OnPing = (socket, member, uid, apiVersion) => {
    socket setAlive uid
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
    uid: Socket.Uid,
    apiVersion: ApiVersion,
    onPing: OnPing = defaultOnPing
  ): JsIteratee = {
    val fullCtrl = controller orElse baseController(hub, socket, member, uid, apiVersion, onPing)
    Iteratee.foreach[JsValue] { v =>
      if (!socket.getIsAlive) {
        // this socket is dead, ignore message and tell client to reconnect to the new socket
        lila.mon.socket.deadMsg()
        member push SocketTrouper.resyncMessage
      } // process null ping immediately
      else if (v == JsNull) onPing(socket, member, uid, apiVersion)
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
      // and it can be called after a reconnection (using same uid) was performed,
      // effectively quitting the reconnected client.
      .map(_ => socket ! Quit(uid, member))
  }

  def recordUserLagFromPing(member: SocketMember, ping: JsObject) = for {
    user <- member.userId
    lag <- (ping \ "l").asOpt[Centis]
  } UserLagCache.put(user, lag)

  private def baseController(
    hub: lila.hub.Env,
    socket: SocketTrouper[_],
    member: SocketMember,
    uid: Socket.Uid,
    apiVersion: ApiVersion,
    onPing: OnPing
  ): Controller = {
    // latency ping, or BC mobile app ping
    case ("p", o) =>
      onPing(socket, member, uid, apiVersion)
      recordUserLagFromPing(member, o)
    case ("following_onlines", _) => member.userId foreach { u =>
      hub.relation ! ReloadOnlineFriends(u)
    }
    case ("startWatching", o) => o str "d" foreach { ids =>
      hub.bus.publish(StartWatching(uid, member, ids.split(' ').toSet), 'socketMoveBroadcast)
    }
    case ("moveLat", o) => hub.bus.publish(
      if (~(o boolean "d")) Channel.Sub(member) else Channel.UnSub(member),
      'roundMoveTimeChannel
    )
    case ("anaMove", o) => AnaRateLimit(uid, member) {
      AnaMove parse o foreach { anaMove =>
        member push {
          anaMove.branch match {
            case scalaz.Success(node) => makeMessage("node", anaMove json node)
            case scalaz.Failure(err) => makeMessage("stepFailure", err.toString)
          }
        }
      }
    }
    case ("anaDrop", o) => AnaRateLimit(uid, member) {
      AnaDrop parse o foreach { anaDrop =>
        anaDrop.branch match {
          case scalaz.Success(branch) =>
            member push makeMessage("node", anaDrop json branch)
          case scalaz.Failure(err) =>
            member push makeMessage("stepFailure", err.toString)
        }
      }
    }
    case ("anaDests", o) => AnaRateLimit(uid, member) {
      member push {
        AnaDests parse o match {
          case Some(res) => makeMessage("dests", res.json)
          case None => makeMessage("destsFailure", "Bad dests request")
        }
      }
    }
    case ("opening", o) => AnaRateLimit(uid, member) {
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
