package lila.swiss

import lila.common.LateMultiThrottler
import lila.core.team.IsLeaderWithCommPerm
import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.core.socket.{ protocol as P, * }

final private class SwissSocket(
    socketKit: SocketKit,
    chat: lila.chat.ChatApi,
    teamOf: SwissId => Fu[Option[TeamId]]
)(using Executor, akka.actor.ActorSystem, Scheduler, lila.user.FlairApi.Getter):

  private val reloadThrottler = LateMultiThrottler(executionTimeout = none, logger = logger)

  def reload(id: SwissId): Unit =
    reloadThrottler ! LateMultiThrottler.work(
      id = id,
      run = fuccess:
        send(RP.Out.tellRoom(id.into(RoomId), makeMessage("reload")))
      ,
      delay = 1.seconds.some
    )

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Swiss)

  private lazy val handler: SocketHandler =
    roomHandler(
      rooms,
      chat,
      logger,
      roomId => _.Swiss(SwissId(roomId.value)).some,
      localTimeout = Some: (roomId, modId, _) =>
        teamOf(SwissId(roomId.value)).flatMapz: teamId =>
          lila.common.Bus.ask[Boolean]("teamIsLeader") { IsLeaderWithCommPerm(teamId, modId, _) },
      chatBusChan = _.Swiss
    )

  private lazy val send = socketKit.send("swiss-out")

  socketKit
    .subscribe("swiss-in", RP.In.reader)(handler.orElse(socketKit.baseHandler))
    .andDo(send(P.Out.boot))
