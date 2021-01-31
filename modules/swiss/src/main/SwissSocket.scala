package lila.swiss

import scala.concurrent.duration._

import lila.hub.actorApi.team.IsLeader
import lila.hub.LateMultiThrottler
import lila.hub.LightTeam.TeamID
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.makeMessage

final private class SwissSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: lila.chat.ChatApi,
    teamOf: Swiss.Id => Fu[Option[TeamID]]
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
) {

  private val reloadThrottler = LateMultiThrottler(executionTimeout = none, logger = logger)

  def reload(id: Swiss.Id): Unit =
    reloadThrottler ! LateMultiThrottler.work(
      id = id.value,
      run = fuccess {
        send(RP.Out.tellRoom(RoomId(id.value), makeMessage("reload")))
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
      roomId => _.Swiss(roomId.value).some,
      localTimeout = Some { (roomId, modId, _) =>
        teamOf(Swiss.Id(roomId.value)) flatMap {
          _ ?? { teamId =>
            lila.common.Bus.ask[Boolean]("teamIsLeader") { IsLeader(teamId, modId, _) }
          }
        }
      },
      chatBusChan = _.Swiss
    )

  private lazy val send: String => Unit = remoteSocketApi.makeSender("swiss-out").apply _

  remoteSocketApi.subscribe("swiss-in", RP.In.reader)(
    handler orElse remoteSocketApi.baseHandler
  ) >>- send(P.Out.boot)
}
