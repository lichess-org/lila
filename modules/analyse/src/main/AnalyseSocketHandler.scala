package lidraughts.analyse

import lidraughts.socket._
import lidraughts.user.User

import play.api.libs.iteratee._
import play.api.libs.json.JsValue

private[analyse] final class AnalyseSocketHandler(
    socket: lidraughts.hub.Trouper,
    hub: lidraughts.hub.Env,
    evalCacheHandler: lidraughts.evalCache.EvalCacheSocketHandler
) {

  import AnalyseSocket._

  def join(uid: Socket.Uid, user: Option[User], apiVersion: Int): Fu[JsSocketHandler] = {
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
