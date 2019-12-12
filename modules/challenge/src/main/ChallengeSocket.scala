package lila.challenge

import play.api.libs.json._
import scala.concurrent.duration._

import lila.game.Pov
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.makeMessage

private final class ChallengeSocket(
    api: ChallengeApi,
    remoteSocketApi: lila.socket.RemoteSocket
) {

  import ChallengeSocket._

  def reload(challengeId: Challenge.ID): Unit =
    rooms.tell(challengeId, NotifyVersion("reload", JsNull))

  lazy val rooms = makeRoomMap(send, false)

  private lazy val send: String => Unit = remoteSocketApi.makeSender("chal-out").apply _

  private lazy val challengeHandler: Handler = {
    case Protocol.In.OwnerPings(ids) => ids foreach api.ping
  }

  remoteSocketApi.subscribe("chal-in", Protocol.In.reader)(
    challengeHandler orElse minRoomHandler(rooms, lila log "challenge") orElse remoteSocketApi.baseHandler
  )

  api registerSocket this
}

object ChallengeSocket {

  object Protocol {

    object In {

      case class OwnerPings(ids: Iterable[String]) extends P.In

      val reader: P.In.Reader = raw => raw.path match {
        case "challenge/pings" => OwnerPings(P.In.commas(raw.args)).some
        case _ => RP.In.reader(raw)
      }
    }
  }
}
