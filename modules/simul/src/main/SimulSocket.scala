package lila.simul

import akka.actor.ActorSelection
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.game.{ Game, Pov }
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.makeMessage

private final class SimulSocket(
    getSimul: Simul.ID => Fu[Option[Simul]],
    jsonView: JsonView,
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: ActorSelection,
    bus: lila.common.Bus
) {

  def hostIsOn(simulId: Simul.ID, gameId: Game.ID): Unit =
    rooms.tell(simulId, NotifyVersion("hostGame", gameId))

  def reload(simulId: Simul.ID): Unit =
    getSimul(simulId) foreach {
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

  private def redirectPlayer(simul: Simul, pov: Pov): Unit =
    pov.player.userId foreach { userId =>
      send(RP.Out.tellRoomUser(RoomId(simul.id), userId, makeMessage("redirect", pov.fullId)))
    }

  lazy val rooms = makeRoomMap(send, bus)

  private lazy val handler: Handler = roomHandler(rooms, chat, logger,
    roomId => _.Simul(roomId.value).some)

  private lazy val send: String => Unit = remoteSocketApi.makeSender("simul-out").apply _

  remoteSocketApi.subscribe("simul-in", RP.In.reader)(handler orElse remoteSocketApi.baseHandler)
}
