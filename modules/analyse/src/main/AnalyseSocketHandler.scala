package lidraughts.analyse

import lidraughts.socket._
import lidraughts.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: AnalyseSocket,
    hub: lidraughts.hub.Env,
    evalCacheHandler: lidraughts.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  def join(uid: Socket.Uid, user: Option[User], apiVersion: Int): Fu[JsSocketHandler] =
    socket.addMember(uid)(Member(_, user.map(_.id))) map {
      case (member, enum) => Handler.iteratee(
        hub,
        evalCacheHandler(uid, member, user),
        member,
        socket,
        uid
      ) -> enum
    }
}
