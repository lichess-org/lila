package lila.round

import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.game.Game
import lila.socket.RemoteSocket.{ Protocol => P, _ }

final class RoundRemoteSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: akka.actor.ActorSelection,
    bus: lila.common.Bus
) {

  import RoundRemoteSocket._

  lazy val rooms = makeRoomMap(send, bus)

  private lazy val roundHandler: Handler = {
    case Protocol.In.TellRoundSri(gameId, P.In.TellSri(sri, user, tpe, o)) =>
      tpe match {
        // case ("talk", o) => for {
        //   line <- o str "d"
        //   u <- user
        // } messenger.watcher(gameId, u, line)
        case t => logger.warn(s"Unhandled round socket message: $t")
      }
  }

  private lazy val rHandler: Handler = roomHandler(rooms, chat, roomId => _.Watcher(roomId.value).some)

  private lazy val send: String => Unit = remoteSocketApi.makeSender("round-out").apply _

  remoteSocketApi.subscribe("round-in", Protocol.In.reader)(
    rHandler orElse remoteSocketApi.baseHandler
  )
}

object RoundRemoteSocket {

  object Protocol {

    object In {

      case class TellRoundSri(gameId: Game.ID, tellSri: P.In.TellSri) extends P.In

      val reader: P.In.Reader = raw => roundReader(raw) orElse RP.In.reader(raw)

      val roundReader: P.In.Reader = raw => raw.path match {
        case "tell/round/sri" => raw.get(4) {
          case arr @ Array(gameId, _, _, _) => P.In.tellSriMapper.lift(arr drop 1).flatten map {
            TellRoundSri(gameId, _)
          }
        }
        case _ => none
      }
    }
  }
}
