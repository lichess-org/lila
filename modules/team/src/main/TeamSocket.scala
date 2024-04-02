package lila.team

import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.core.socket.{ protocol as P, * }

final private class TeamSocket(
    socketKit: SocketKit,
    chat: lila.chat.ChatApi,
    api: TeamApi
)(using Executor, lila.user.FlairApi.Getter):

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Team)

  private lazy val handler: SocketHandler = roomHandler(
    rooms,
    chat,
    logger,
    roomId => _.Team(roomId.into(TeamId)).some,
    localTimeout = Some: (roomId, modId, suspectId) =>
      api.hasPerm(roomId.into(TeamId), modId, _.Comm) >>&
        api.hasPerm(roomId.into(TeamId), suspectId, _.Comm).not,
    chatBusChan = _.Team
  )

  private lazy val send = socketKit.send("team-out")

  socketKit
    .subscribe("team-in", RP.In.reader)(handler.orElse(socketKit.baseHandler))
    .andDo(send(P.Out.boot))
