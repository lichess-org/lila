package lila.site

import scala.concurrent.duration._

import akka.actor._
import play.api.libs.json._
import play.api.libs.streams.ActorFlow

import actorApi._
import lila.common.PimpedJson._
import lila.socket._
import lila.socket.actorApi.StartWatching

private[site] final class SocketHandler(
    socket: ActorRef,
    hub: lila.hub.Env)(implicit val system: ActorSystem) {

  def apply(
    uid: String,
    userId: Option[String],
    flag: Option[String]): JsFlow =
    ActorFlow.actorRef[JsValue, JsValue] { out =>
      val member = Member(out, userId, flag)
      socket ! AddMember(uid, member)
      val controller = Handler.completeController(hub, socket, member, uid, userId)(Handler.emptyController)
      lila.socket.SocketMemberActor.props(out, controller)
    }

}
