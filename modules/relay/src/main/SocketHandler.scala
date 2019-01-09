package lila.relay

import lila.common.ApiVersion
import lila.socket.Socket.{ Uid, SocketVersion }
import lila.socket.{ Handler, JsSocketHandler }
import lila.study.{ Study, StudySocket, SocketHandler => StudyHandler }
import lila.user.User

private[relay] final class SocketHandler(
    studyHandler: StudyHandler,
    api: RelayApi
) {

  private def makeController(
    socket: StudySocket,
    relayId: Relay.Id,
    uid: Uid,
    member: StudySocket.Member,
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
    user: Option[User],
    version: Option[SocketVersion],
    apiVersion: ApiVersion
  ): Fu[JsSocketHandler] = {
    val studyId = Study.Id(relayId.value)
    val socket = studyHandler.getSocket(studyId)
    studyHandler.join(studyId, uid, user, socket, member => makeController(socket, relayId, uid, member, user), version, apiVersion)
  }
}
