package lidraughts.analyse

import lidraughts.socket._
import lidraughts.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: lidraughts.hub.Trouper,
    hub: lidraughts.hub.Env,
    evalCacheHandler: lidraughts.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  def join(uid: Socket.Uid, user: Option[User], apiVersion: Int): Fu[JsSocketHandler] =
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
