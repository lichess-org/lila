package lidraughts.relay

import akka.actor._

import lidraughts.socket.Socket.Uid
import lidraughts.socket.{ Handler, JsSocketHandler }
import lidraughts.study.{ Study, Socket, SocketHandler => StudyHandler }
import lidraughts.user.User

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
    case ("relaySync", o) =>
      logger.info(s"${user.fold("Anon")(_.username)} toggles #${relayId}")
      api.requestPlay(relayId, ~(o \ "d").asOpt[Boolean])
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
