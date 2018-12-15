package lidraughts.analyse

import lidraughts.common.ApiVersion
import lidraughts.socket._
import lidraughts.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: AnalyseSocket,
    hub: lidraughts.hub.Env,
    evalCacheHandler: lidraughts.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  def join(
    uid: Socket.Uid,
    user: Option[User],
    apiVersion: ApiVersion
  ): Fu[JsSocketHandler] =
    socket.ask[Connected](Join(uid, user.map(_.id), _)) map {
      case Connected(enum, member) => Handler.iteratee(
        hub,
        evalCacheHandler(uid, member, user),
        member,
        socket,
        uid,
        apiVersion
      ) -> enum
    }
}
