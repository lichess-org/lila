package lila.tournament

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.game.{ Game, Pov }
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.makeMessage

private final class RemoteTournamentSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: ActorSelection,
    system: ActorSystem
) {

  lazy val rooms = makeRoomMap(send, system.lilaBus)

  private lazy val handler: Handler = roomHandler(rooms, chat,
    roomId => lila.hub.actorApi.shutup.PublicSource.Tournament(roomId.value).some)

  private lazy val send: String => Unit = remoteSocketApi.makeSender("tour-out").apply _

  remoteSocketApi.subscribe("tour-in", P.In.baseReader)(handler orElse remoteSocketApi.baseHandler)
}
