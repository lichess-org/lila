package lila.simul

import play.api.libs.json._

import lila.game.{ Game, Pov }
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.makeMessage
import lila.user.User

final private class SimulSocket(
    repo: SimulRepo,
    jsonView: JsonView,
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: lila.chat.ChatApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mode: play.api.Mode
) {

  def hostIsOn(simulId: Simul.ID, gameId: Game.ID): Unit =
    rooms.tell(simulId, NotifyVersion("hostGame", gameId))

  def reload(simulId: Simul.ID): Unit =
    repo find simulId foreach {
      _ foreach { simul =>
        jsonView(simul, none) foreach { obj =>
          rooms.tell(simulId, NotifyVersion("reload", obj))
        }
      }
    }

  def aborted(simulId: Simul.ID): Unit =
    rooms.tell(simulId, NotifyVersion("aborted", Json.obj()))

  def startSimul(simul: Simul, firstGame: Game): Unit =
    firstGame.playerByUserId(simul.hostId) foreach { player =>
      redirectPlayer(simul, Pov(firstGame, player))
    }

  def startGame(simul: Simul, game: Game): Unit =
    game.playerByUserId(simul.hostId) foreach { opponent =>
      redirectPlayer(simul, Pov(game, !opponent.color))
    }

  def filterPresent(simul: Simul, userIds: Set[User.ID]): Fu[Seq[User.ID]] =
    remoteSocketApi.request[Seq[User.ID]](
      id => send(SimulSocket.Protocol.Out.filterPresent(id, simul.id, userIds)),
      userIds => lila.socket.RemoteSocket.Protocol.In.commas(userIds).toSeq
    )

  private def redirectPlayer(simul: Simul, pov: Pov): Unit =
    pov.player.userId foreach { userId =>
      send(RP.Out.tellRoomUser(RoomId(simul.id), userId, makeMessage("redirect", pov.fullId)))
    }

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Simul)

  private lazy val handler: Handler =
    roomHandler(
      rooms,
      chat,
      logger,
      roomId => _.Simul(roomId.value).some,
      chatBusChan = _.Simul,
      localTimeout = Some { (roomId, modId, _) =>
        repo.hostId(roomId.value).map(_ has modId)
      }
    )

  private lazy val send: String => Unit = remoteSocketApi.makeSender("simul-out").apply _

  remoteSocketApi.subscribe("simul-in", RP.In.reader)(
    handler orElse remoteSocketApi.baseHandler
  ) >>- send(P.Out.boot)
}

private object SimulSocket {
  object Protocol {
    object Out {
      import lila.socket.RemoteSocket.Protocol.Out.commas
      def filterPresent(reqId: Int, simulId: Simul.ID, userIds: Set[User.ID]) =
        s"room/filter-present $reqId $simulId ${commas(userIds)}"
    }
  }
}
