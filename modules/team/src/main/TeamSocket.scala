package lila.team

import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.socket.RemoteSocket.{ Protocol as P, * }

final private class TeamSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: lila.chat.ChatApi,
    cached: Cached
)(using Executor):

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Team)

  private lazy val handler: Handler = roomHandler(
    rooms,
    chat,
    logger,
    roomId => _.Team(roomId into TeamId).some,
    localTimeout = Some { (roomId, modId, suspectId) =>
      cached.isLeader(roomId into TeamId, modId) >>& !cached.isLeader(roomId into TeamId, suspectId)
    },
    chatBusChan = _.Team
  )

  private lazy val send: String => Unit = remoteSocketApi.makeSender("team-out").apply

  remoteSocketApi.subscribe("team-in", RP.In.reader)(
    handler orElse remoteSocketApi.baseHandler
  ) >>- send(P.Out.boot)
