package lila.analyse

import lila.socket._
import lila.user.User

import play.api.libs.iteratee._
import play.api.libs.json.JsValue

private[analyse] final class AnalyseSocketHandler(
    socket: lila.hub.Trouper,
    hub: lila.hub.Env,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  def join(uid: Socket.Uid, user: Option[User]): Fu[JsSocketHandler] = {
    val (enumerator, channel) = Concurrent.broadcast[JsValue]
    val member = Member(channel, user.map(_.id))
    socket.ask[Unit](AddMember(uid, member, _)) map { _ =>
      Handler.iteratee(
        hub,
        evalCacheHandler(uid, member, user),
        member,
        socket,
        uid
      ) -> enumerator
    }
  }
}
