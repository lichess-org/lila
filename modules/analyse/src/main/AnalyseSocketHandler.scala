package lidraughts.analyse

import lidraughts.socket._
import lidraughts.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: akka.actor.ActorRef,
    hub: lidraughts.hub.Env,
    evalCacheHandler: lidraughts.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  private def controller(member: Member, user: Option[User]): Handler.Controller =
    evalCacheHandler(member, user)

  def join(uid: Socket.Uid, user: Option[User]): Fu[JsSocketHandler] =
    Handler(hub, socket, uid, Join(uid, user.map(_.id))) {
      case Connected(enum, member) => (controller(member, user), enum, member)
    }
}
