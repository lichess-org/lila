package lila.analyse

import akka.actor._
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.socket._
import lila.socket.Socket.makeMessage
import lila.user.User

private[analyse] final class AnalyseSocketHandler(
    socket: akka.actor.ActorRef,
    hub: lila.hub.Env,
    evalCache: lila.evalCache.EvalCacheApi) {

  import AnalyseSocket._
  import Handler.AnaRateLimit

  private def controller(
    socket: ActorRef,
    uid: Socket.Uid,
    member: Member,
    user: Option[User]): Handler.Controller = ({

    case ("anaDests", o) => AnaRateLimit(uid.value, member) {
      AnaDests.parse(o).map(_.compute) match {
        case None => member push makeMessage("destsFailure", "Bad dests request")
        case Some(res) if res.ply < 10 =>
          evalCache.getEvalJson(res) foreach { eval =>
            member push makeMessage("dests", res.json.add("eval" -> eval))
          }
        case Some(res) => member push makeMessage("dests", res.json)
      }
    }

    case ("anaMove", o) => AnaRateLimit(uid.value, member) {
      AnaMove parse o foreach { anaMove =>
        anaMove.branch match {
          case scalaz.Failure(err) => member push makeMessage("stepFailure", err.toString)
          case scalaz.Success(node) if node.ply < 10 =>
            evalCache.getEvalJson(node, anaMove.multiPv) foreach { eval =>
              member push makeMessage("node", anaMove.json(node).add("eval" -> eval))
            }
          case scalaz.Success(node) => member push makeMessage("node", anaMove json node)
        }
      }
    }
  }: Handler.Controller) orElse evalCache.socketHandler(user)

  def join(uid: Socket.Uid, user: Option[User]): Fu[JsSocketHandler] =
    Handler(hub, socket, uid, Join(uid, user.map(_.id))) {
      case Connected(enum, member) => (controller(socket, uid, member, user), enum, member)
    }
}
