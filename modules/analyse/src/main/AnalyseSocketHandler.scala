package lila.analyse

import akka.actor._
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.socket._
import lila.socket.Socket.makeMessage
import lila.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: akka.actor.ActorRef,
    hub: lila.hub.Env,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler) {

  import AnalyseSocket._
  import Handler.AnaRateLimit

  private def controller(member: Member, user: Option[User]): Handler.Controller =
    evalCacheHandler(member, user)

  def join(uid: Socket.Uid, user: Option[User]): Fu[JsSocketHandler] =
    Handler(hub, socket, uid, Join(uid, user.map(_.id))) {
      case Connected(enum, member) => (controller(member, user), enum, member)
    }
}
