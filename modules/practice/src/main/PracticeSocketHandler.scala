package lila.practice

import akka.actor._
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.socket._
import lila.user.User

private[practice] final class PracticeSocketHandler(
    socket: akka.actor.ActorRef,
    hub: lila.hub.Env,
    evalCache: lila.evalCache.EvalCacheApi) {

  import PracticeSocketHandler._
  import Handler.AnaRateLimit

  private def controller(
    socket: ActorRef,
    uid: Socket.Uid,
    member: Member,
    user: Option[User]): Handler.Controller = ({

    case ("anaDests", o) => AnaRateLimit(uid.value, member) {
      AnaDests.parse(o).map(_.compute) match {
        case None => member push Socket.makeMessage("destsFailure", "Bad dests request")
        case Some(res) =>
          evalCache.getEvalJson(res) foreach { eval =>
            member push Socket.makeMessage("dests", res.json.add("eval" -> eval))
          }
      }
    }
  }: Handler.Controller) orElse evalCache.socketHandler(user)

  def join(uid: Socket.Uid, user: Option[User]): Fu[JsSocketHandler] = {

    Handler(hub, socket, uid, Join(uid, user.map(_.id))) {
      case Connected(enum, member) => (Handler.emptyController, enum, member)
    }
  }
}

private object PracticeSocketHandler {

  case class Member(
      channel: JsChannel,
      userId: Option[String]) extends SocketMember {

    val troll = false
  }

  case class Join(uid: Socket.Uid, userId: Option[User.ID])
  private[practice] case class Connected(enumerator: JsEnumerator, member: Member)
}
