package lila.challenge

import play.api.libs.json._
import scala.concurrent.duration._

import lila.game.Pov
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.makeMessage

private final class ChallengeSocket(
    api: ChallengeApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    bus: lila.common.Bus
) {

  import ChallengeSocket._

  def reload(challengeId: Challenge.ID): Unit =
    rooms.tell(challengeId, NotifyVersion("reload", JsNull))

  lazy val rooms = makeRoomMap(send, bus)

  private lazy val send: String => Unit = remoteSocketApi.makeSender("chal-out").apply _

  private lazy val challengeHandler: Handler = {
    case Protocol.In.OwnerPing(roomId) => api ping roomId.value
  }

  remoteSocketApi.subscribe("chal-in", Protocol.In.reader)(
    challengeHandler orElse minRoomHandler(rooms) orElse remoteSocketApi.baseHandler
  )

  api registerSocket this
}

object ChallengeSocket {

  object Protocol {

    object In {

      case class OwnerPing(roomId: RoomId) extends P.In

      val reader: P.In.Reader = raw => raw.path match {
        case "challenge/ping" => OwnerPing(RoomId(raw.args)).some
        case _ => RP.In.reader(raw)
      }
    }
  }
}
