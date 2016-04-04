package lila.socket

import akka.actor.{ ActorRef, Props, ActorSystem }
import akka.pattern.{ ask, pipe }
import akka.stream._
import akka.stream.scaladsl._
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import scala.concurrent.duration._

import actorApi._
import lila.common.PimpedJson._
import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.socket.Socket.makeMessage
import makeTimeout.large

object Handler {

  type Controller = PartialFunction[(String, JsObject), Unit]

  val emptyController: Controller = PartialFunction.empty

  lazy val AnaRateLimit = new lila.memo.RateLimit(90, 60 seconds, "socket analysis move")

  def actorRef(withOut: ActorRef => Props)(implicit system: ActorSystem): JsFlow =
    ActorFlow.actorRef[JsObject, JsObject](withOut)

  def props(
    hub: lila.hub.Env,
    socket: ActorRef,
    member: SocketMember,
    uid: String,
    userId: Option[String])(controller: Controller): Props = {
    val control = controller orElse baseController(hub, socket, member, uid, userId)
    lila.socket.SocketMemberActor.props(in =>
      in str "t" foreach { t =>
        control.lift(t -> in)
      }
    )
  }

  def baseController(
    hub: lila.hub.Env,
    socket: ActorRef,
    member: SocketMember,
    uid: String,
    userId: Option[String]): Controller = {
    case ("p", _) => socket ! Ping(uid)
    case ("following_onlines", _) => userId foreach { u =>
      hub.actor.relation ! ReloadOnlineFriends(u)
    }
    case ("startWatching", o) => o str "d" foreach { ids =>
      hub.actor.moveBroadcast ! StartWatching(uid, member, ids.split(' ').toSet)
    }
    case ("moveLat", o) => hub.channel.roundMoveTime ! (~(o boolean "d")).fold(
      Channel.Sub(member),
      Channel.UnSub(member))
    case ("anaMove", o) => AnaRateLimit(uid) {
      AnaMove parse o foreach { anaMove =>
        anaMove.step match {
          case scalaz.Success(step) =>
            member push makeMessage("step", Json.obj(
              "step" -> step.toJson,
              "path" -> anaMove.path
            ))
          case scalaz.Failure(err) =>
            member push makeMessage("stepFailure", err.toString)
        }
      }
    }
    case ("anaDrop", o) => AnaRateLimit(uid) {
      AnaDrop parse o foreach { anaDrop =>
        anaDrop.step match {
          case scalaz.Success(step) =>
            member push makeMessage("step", Json.obj(
              "step" -> step.toJson,
              "path" -> anaDrop.path
            ))
          case scalaz.Failure(err) =>
            member push makeMessage("stepFailure", err.toString)
        }
      }
    }
    case ("anaDests", o) => AnaRateLimit(uid) {
      import Step.openingWriter
      AnaDests parse o match {
        case Some(req) =>
          member push makeMessage("dests", Json.obj(
            "dests" -> req.dests,
            "path" -> req.path
          ) ++ req.opening.?? { o =>
              Json.obj("opening" -> o)
            })
        case None =>
          member push makeMessage("destsFailure", "Bad dests request")
      }
    }
    case _ => // logwarn("Unhandled msg: " + msg)
  }
}
