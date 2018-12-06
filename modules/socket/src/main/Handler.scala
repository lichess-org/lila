package lila.socket

import akka.actor.ActorRef
import akka.pattern.ask
import ornicar.scalalib.Zero
import play.api.libs.iteratee.Iteratee
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.hub.Trouper
import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.socket.Socket.makeMessage
import makeTimeout.large

object Handler {

  type Controller = PartialFunction[(String, JsObject), Unit]
  type Connecter = PartialFunction[Any, (Controller, JsEnumerator, SocketMember)]

  val emptyController: Controller = PartialFunction.empty

  private val AnaRateLimiter = new lila.memo.RateLimit[String](120, 30 seconds,
    name = "socket analysis move",
    key = "socket_analysis_move")

  def AnaRateLimit[A: Zero](uid: Socket.Uid, member: SocketMember)(op: => A) =
    AnaRateLimiter(uid.value, msg = s"user: ${member.userId | "anon"}")(op)

  private trait SocketAdapter {
    def !(msg: Any): Unit
    def askJoin(j: Any): Fu[Any]
  }
  private def actorAdapter(ref: ActorRef) = new SocketAdapter {
    def !(msg: Any) = ref ! msg
    def askJoin(j: Any) = ref ? j
  }
  private def trouperAdapter(trouper: lila.hub.Trouper) = new SocketAdapter {
    def !(msg: Any) = trouper ! msg
    def askJoin(j: Any) = trouper ? j
  }

  def forActor(
    hub: lila.hub.Env,
    socket: ActorRef,
    uid: Socket.Uid,
    join: Any
  )(connecter: Connecter) = apply[ActorRef](hub, socket, uid, join)(actorAdapter)(connecter)

  def forTrouper(
    hub: lila.hub.Env,
    socket: Trouper,
    uid: Socket.Uid,
    join: Any
  )(connecter: Connecter) = apply[Trouper](hub, socket, uid, join)(trouperAdapter)(connecter)

  private def apply[S](
    hub: lila.hub.Env,
    anySocket: S,
    uid: Socket.Uid,
    join: Any
  )(socketAdapter: S => SocketAdapter)(connecter: Connecter): Fu[JsSocketHandler] = {

    val socket = socketAdapter(anySocket)

    def baseController(member: SocketMember): Controller = {
      case ("p", o) => socket ! Ping(uid, o)
      case ("following_onlines", _) => member.userId foreach { u =>
        hub.actor.relation ! ReloadOnlineFriends(u)
      }
      case ("startWatching", o) => o str "d" foreach { ids =>
        hub.actor.moveBroadcast ! StartWatching(uid, member, ids.split(' ').toSet)
      }
      case ("moveLat", o) => hub.channel.roundMoveTime ! {
        if (~(o boolean "d")) Channel.Sub(member) else Channel.UnSub(member)
      }
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
        hub.actor.notification ! lila.hub.actorApi.notify.Notified(userId)
      }
      case _ => // logwarn("Unhandled msg: " + msg)
    }

    def iteratee(controller: Controller, member: SocketMember): JsIteratee = {
      val control = controller orElse baseController(member)
      Iteratee.foreach[JsValue](jsv =>
        jsv.asOpt[JsObject] foreach { obj =>
          obj str "t" foreach { t =>
            control.lift(t -> obj)
          }
        })
        // Unfortunately this map function is only called
        // if the JS closes the socket with lichess.socket.disconnect()
        // but not if the tab is closed or browsed away!
        .map(_ => socket ! Quit(uid))
    }

    socket askJoin join map connecter map {
      case (controller, enum, member) => iteratee(controller, member) -> enum
    }
  }
}
