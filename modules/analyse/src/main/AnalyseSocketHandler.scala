package lidraughts.analyse

import lidraughts.socket._
import lidraughts.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: akka.actor.ActorRef,
    hub: lidraughts.hub.Env,
    evalCacheHandler: lidraughts.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  def join(uid: Socket.Uid, user: Option[User], apiVersion: Int): Fu[JsSocketHandler] =
    Handler(hub, socket, uid, Join(uid, user.map(_.id))) {
      case Connected(enum, member) => (evalCacheHandler(uid, member, user), enum, member)
    }
}
