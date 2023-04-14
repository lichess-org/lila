package lila.challenge

import play.api.libs.json.*

import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.socket.RemoteSocket.{ Protocol as P, * }

final private class ChallengeSocket(
    api: ChallengeApi,
    remoteSocketApi: lila.socket.RemoteSocket
)(using Executor):

  import ChallengeSocket.*

  def reload(challengeId: Challenge.Id): Unit =
    rooms.tell(challengeId into RoomId, NotifyVersion("reload", JsNull))

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

      case class OwnerPings(ids: Iterable[Challenge.Id]) extends P.In

      val reader: P.In.Reader = raw =>
        raw.path match
          case "challenge/pings" => OwnerPings(Challenge.Id from P.In.commas(raw.args)).some
          case _                 => RP.In.reader(raw)
