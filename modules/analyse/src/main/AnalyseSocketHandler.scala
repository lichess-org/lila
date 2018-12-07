package lila.analyse

import lila.socket._
import lila.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: AnalyseSocket,
    hub: lila.hub.Env,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  def join(uid: Socket.Uid, user: Option[User]): Fu[JsSocketHandler] =
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
