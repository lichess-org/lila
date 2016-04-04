package lila.site

import akka.actor._
import play.api.libs.json._

import actorApi._
import lila.socket._

private[site] final class SocketHandler(
    socket: ActorRef,
    hub: lila.hub.Env)(implicit val system: ActorSystem) {

  def apply(uid: String, userId: Option[String], flag: Option[String]): JsFlow =
    Handler.actorRef { out =>
      val member = Member(out, userId, flag)
      socket ! AddMember(uid, member)
      Handler.props(hub, socket, member, uid, userId)(Handler.emptyController)
    }
}
