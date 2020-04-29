package lila.swiss

import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.hub.actorApi.team.IsLeader

final private class SwissSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: lila.chat.ChatApi
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem, mode: play.api.Mode) {

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Swiss)

  private lazy val handler: Handler =
    roomHandler(
      rooms,
      chat,
      logger,
      roomId => _.Swiss(roomId.value).some,
      localTimeout = Some { (roomId, modId, suspectId) =>
        lila.common.Bus.ask[Boolean]("teamIsLeader") { IsLeader(roomId.value, modId, _) }
      },
      chatBusChan = _.Swiss
    )

  private lazy val send: String => Unit = remoteSocketApi.makeSender("swiss-out").apply _

  remoteSocketApi.subscribe("swiss-in", RP.In.reader)(
    handler orElse remoteSocketApi.baseHandler
  ) >>- send(P.Out.boot)
}
