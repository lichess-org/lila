package lila.relay

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import play.api.libs.json._

import lila.chat.Chat
import lila.socket.Socket.Uid
import lila.socket.{ Handler, JsSocketHandler }
import lila.study.{ Study, Socket, SocketHandler => StudyHandler }
import lila.user.User
import makeTimeout.short

private[relay] final class SocketHandler(
    studyHandler: StudyHandler,
    api: RelayApi
) {

  private def makeController(
    socket: ActorRef,
    relayId: Relay.Id,
    uid: Uid,
    member: Socket.Member,
    user: Option[User]
  ): Handler.Controller = ({
    case ("relaySync", o) => user foreach { u =>
      api.setSync(relayId, u, ~(o \ "d").asOpt[Boolean])
    }
  }: Handler.Controller) orElse studyHandler.makeController(
    socket = socket,
    studyId = Study.Id(relayId.value),
    uid = uid,
    member = member,
    user = user
  )

  def join(
    relayId: Relay.Id,
    uid: Uid,
    user: Option[User]
  ): Fu[Option[JsSocketHandler]] = {
    val studyId = Study.Id(relayId.value)
    studyHandler.getSocket(studyId) flatMap { socket =>
      studyHandler.join(studyId, uid, user, socket, member => makeController(socket, relayId, uid, member, user))
    }
  }
}
