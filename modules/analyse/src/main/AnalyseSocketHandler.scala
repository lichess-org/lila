package lila.analyse

import lila.socket._
import lila.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: lila.hub.Trouper,
    hub: lila.hub.Env,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  def join(uid: Socket.Uid, user: Option[User]): Fu[JsSocketHandler] =
    socket.ask[Connected](JoinP(uid, user.map(_.id), _)) map {
      case Connected(enum, member) => Handler.iteratee(
        hub,
        evalCacheHandler(uid, member, user),
        member,
        socket,
        uid
      ) -> enum
    }
}
