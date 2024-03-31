package lila.challenge

import play.api.libs.json.*

import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.core.socket.{ protocol as P, * }

final private class ChallengeSocket(
    api: ChallengeApi,
    socketKit: SocketKit
)(using Executor):

  import ChallengeSocket.*

  def reload(challengeId: Challenge.Id): Unit =
    rooms.tell(challengeId.into(RoomId), NotifyVersion("reload", JsNull))

  private lazy val send = socketKit.send("chal-out")

  lazy val rooms = makeRoomMap(send)

  private lazy val challengeHandler: SocketHandler = { case Protocol.In.OwnerPings(ids) =>
    ids.foreach(api.ping)
  }

  socketKit.subscribe("chal-in", Protocol.In.reader.orElse(RP.In.reader))(
    challengeHandler.orElse(minRoomHandler(rooms, lila.log("challenge"))).orElse(socketKit.baseHandler)
  )

  api.registerSocket(this)

object ChallengeSocket:

  object Protocol:

    object In:

      case class OwnerPings(ids: Iterable[Challenge.Id]) extends P.In

      val reader: P.In.Reader =
        case P.RawMsg("challenge/pings", raw) => OwnerPings(Challenge.Id.from(P.In.commas(raw.args))).some
