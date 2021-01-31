package lila.team

import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }

final private class TeamSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: lila.chat.ChatApi,
    cached: Cached
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mode: play.api.Mode
) {

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Team)

  private lazy val handler: Handler =
    roomHandler(
      rooms,
      chat,
      logger,
      roomId => _.Team(roomId.value).some,
      localTimeout = Some { (roomId, modId, suspectId) =>
        cached.isLeader(roomId.value, modId) >>& !cached.isLeader(roomId.value, suspectId)
      },
      chatBusChan = _.Team
    )

  private lazy val send: String => Unit = remoteSocketApi.makeSender("team-out").apply _

  remoteSocketApi.subscribe("team-in", RP.In.reader)(
    handler orElse remoteSocketApi.baseHandler
  ) >>- send(P.Out.boot)
}
