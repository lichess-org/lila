package lila.swiss

import lila.hub.actorApi.team.IsLeaderWithCommPerm
import lila.hub.LateMultiThrottler
import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.socket.RemoteSocket.{ Protocol as P, * }
import lila.socket.Socket.makeMessage

final private class SwissSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: lila.chat.ChatApi,
    teamOf: SwissId => Fu[Option[TeamId]]
)(using
    ec: Executor,
    system: akka.actor.ActorSystem,
    scheduler: Scheduler
):

  private val reloadThrottler = LateMultiThrottler(executionTimeout = none, logger = logger)

  def reload(id: SwissId): Unit =
    reloadThrottler ! LateMultiThrottler.work(
      id = id,
      run = fuccess {
        send(RP.Out.tellRoom(id into RoomId, makeMessage("reload")))
      },
      delay = 1.seconds.some
    )

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Swiss)

  private lazy val handler: Handler =
    roomHandler(
      rooms,
      chat,
      logger,
      roomId => _.Swiss(SwissId(roomId.value)).some,
      localTimeout = Some: (roomId, modId, _) =>
        teamOf(SwissId(roomId.value)) flatMapz { teamId =>
          lila.common.Bus.ask[Boolean]("teamIsLeader") { IsLeaderWithCommPerm(teamId, modId, _) }
        },
      chatBusChan = _.Swiss
    )

  private lazy val send: String => Unit = remoteSocketApi.makeSender("swiss-out").apply

  remoteSocketApi.subscribe("swiss-in", RP.In.reader)(
    handler orElse remoteSocketApi.baseHandler
  ) andDo send(P.Out.boot)
