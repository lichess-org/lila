package lila.socket

import akka.pattern.{ ask, pipe }
import ornicar.scalalib.Zero
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.common.PimpedJson._
import lila.hub.actorApi.relation.ReloadOnlineFriends
import makeTimeout.large
import tree.Node.defaultNodeJsonWriter
import tree.Node.openingWriter

object Handler {

  type Controller = PartialFunction[(String, JsObject), Unit]
  type Connecter = PartialFunction[Any, (Controller, JsEnumerator, SocketMember)]

  val emptyController: Controller = PartialFunction.empty

  private val AnaRateLimiter = new lila.memo.RateLimit(120, 30 seconds,
    name = "socket analysis move",
    key = "socket_analysis_move")

  def AnaRateLimit[A: Zero](uid: String, member: SocketMember)(op: => A) =
    AnaRateLimiter(uid, msg = s"user: ${member.userId | "anon"}")(op)

  def apply(
    hub: lila.hub.Env,
    socket: akka.actor.ActorRef,
    uid: String,
    join: Any)(connecter: Connecter): Fu[JsSocketHandler] = {

    def baseController(member: SocketMember): Controller = {
      case ("p", _) => socket ! Ping(uid)
      case ("following_onlines", _) => member.userId foreach { u =>
        hub.actor.relation ! ReloadOnlineFriends(u)
      }
      case ("startWatching", o) => o str "d" foreach { ids =>
        hub.actor.moveBroadcast ! StartWatching(uid, member, ids.split(' ').toSet)
      }
      case ("moveLat", o) => hub.channel.roundMoveTime ! (~(o boolean "d")).fold(
        Channel.Sub(member),
        Channel.UnSub(member))
      case ("anaMove", o) => AnaRateLimit(uid, member) {
        AnaMove parse o foreach { anaMove =>
          anaMove.branch match {
            case scalaz.Success(branch) =>
              member push lila.socket.Socket.makeMessage("node", Json.obj(
                "node" -> branch,
                "path" -> anaMove.path
              ))
            case scalaz.Failure(err) =>
              member push lila.socket.Socket.makeMessage("stepFailure", err.toString)
          }
        }
      }
      case ("anaDrop", o) => AnaRateLimit(uid, member) {
        AnaDrop parse o foreach { anaDrop =>
          anaDrop.branch match {
            case scalaz.Success(branch) =>
              member push lila.socket.Socket.makeMessage("node", Json.obj(
                "node" -> branch,
                "path" -> anaDrop.path
              ))
            case scalaz.Failure(err) =>
              member push lila.socket.Socket.makeMessage("stepFailure", err.toString)
          }
        }
      }
      case ("anaDests", o) => AnaRateLimit(uid, member) {
        AnaDests parse o map (_.compute) match {
          case Some(req) =>
            member push lila.socket.Socket.makeMessage("dests", Json.obj(
              "dests" -> req.dests,
              "path" -> req.path
            ) ++ req.opening.?? { o =>
                Json.obj("opening" -> o)
              })
          case None =>
            member push lila.socket.Socket.makeMessage("destsFailure", "Bad dests request")
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
        }
      ).map(_ => socket ! Quit(uid))
    }

    socket ? join map connecter map {
      case (controller, enum, member) => iteratee(controller, member) -> enum
    }
  }
}
