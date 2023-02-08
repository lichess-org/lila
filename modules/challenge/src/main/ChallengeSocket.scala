package lila.challenge

import play.api.libs.json.*

import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.socket.RemoteSocket.{ Protocol as P, * }

final private class ChallengeSocket(
    api: ChallengeApi,
    remoteSocketApi: lila.socket.RemoteSocket
)(using scala.concurrent.ExecutionContext, play.api.Mode):

  import ChallengeSocket.*

  def reload(challengeId: Challenge.ID): Unit =
    rooms.tell(RoomId(challengeId), NotifyVersion("reload", JsNull))

  private lazy val send: String => Unit = remoteSocketApi.makeSender("chal-out").apply

  lazy val rooms = makeRoomMap(send)

  private lazy val challengeHandler: Handler = { case Protocol.In.OwnerPings(ids) =>
    ids foreach api.ping
  }

  remoteSocketApi.subscribe("chal-in", Protocol.In.reader)(
    challengeHandler orElse minRoomHandler(rooms, lila log "challenge") orElse remoteSocketApi.baseHandler
  )

  api registerSocket this

object ChallengeSocket:

  object Protocol:

    object In:

      case class OwnerPings(ids: Iterable[String]) extends P.In

      val reader: P.In.Reader = raw =>
        raw.path match
          case "challenge/pings" => OwnerPings(P.In.commas(raw.args)).some
          case _                 => RP.In.reader(raw)
