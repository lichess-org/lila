package lila.analyse

import lila.common.ApiVersion
import lila.socket._
import lila.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: AnalyseSocket,
    hub: lila.hub.Env,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  def join(
    sri: Socket.Sri,
    user: Option[User],
    apiVersion: ApiVersion
  ): Fu[JsSocketHandler] =
    socket.ask[Connected](Join(sri, user.map(_.id), _)) map {
      case Connected(enum, member) => Handler.iteratee(
        hub,
        evalCacheHandler(sri, member, user),
        member,
        socket,
        sri,
        apiVersion
      ) -> enum
    }
}
