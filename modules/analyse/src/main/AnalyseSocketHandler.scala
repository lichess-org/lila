package lila.analyse

import lila.socket._
import lila.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: akka.actor.ActorRef,
    hub: lila.hub.Env,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  def join(uid: Socket.Uid, user: Option[User]): Fu[JsSocketHandler] =
    Handler.forActor(hub, socket, uid, Join(uid, user.map(_.id))) {
      case Connected(enum, member) => (evalCacheHandler(uid, member, user), enum, member)
    }
}
