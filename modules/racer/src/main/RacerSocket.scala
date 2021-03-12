package lila.racer

import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import play.api.libs.json.{ JsObject, Json }

final private class RacerSocket(
    api: RacerApi,
    json: RacerJson,
    remoteSocketApi: lila.socket.RemoteSocket
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mode: play.api.Mode
) {

  import RacerSocket._

  def publishState(race: RacerRace): Unit = send(
    Protocol.Out.publishState(race.id, json state race)
  )

  private lazy val send: String => Unit = remoteSocketApi.makeSender("racer-out").apply _

  lazy val rooms = makeRoomMap(send)

  private lazy val racerHandler: Handler = {
    case Protocol.In.PlayerJoin(raceId, playerId) =>
      api.join(raceId, playerId)
    case Protocol.In.PlayerMoves(raceId, playerId, moves) =>
      api.registerPlayerMoves(raceId, playerId, moves)
    case Protocol.In.PlayerEnd(raceId, playerId) =>
      api.playerEnd(raceId, playerId)
  }

  remoteSocketApi.subscribe("racer-in", Protocol.In.reader)(
    racerHandler orElse minRoomHandler(rooms, logger) orElse remoteSocketApi.baseHandler
  )

  api registerSocket this
}

object RacerSocket {

  object Protocol {

    object In {

      case class PlayerJoin(race: RacerRace.Id, player: RacerPlayer.Id)              extends P.In
      case class PlayerMoves(race: RacerRace.Id, player: RacerPlayer.Id, moves: Int) extends P.In
      case class PlayerEnd(race: RacerRace.Id, player: RacerPlayer.Id)               extends P.In

      val reader: P.In.Reader = raw => raceReader(raw) orElse RP.In.reader(raw)

      val raceReader: P.In.Reader = raw =>
        raw.path match {
          case "racer/join" =>
            raw.get(2) { case Array(raceId, playerId) =>
              PlayerJoin(RacerRace.Id(raceId), RacerPlayer.Id(playerId)).some
            }
          case "racer/moves" =>
            raw.get(3) { case Array(raceId, playerId, moveStr) =>
              moveStr.toIntOption map { PlayerMoves(RacerRace.Id(raceId), RacerPlayer.Id(playerId), _) }
            }
          case "racer/end" =>
            raw.get(2) { case Array(raceId, playerId) =>
              PlayerEnd(RacerRace.Id(raceId), RacerPlayer.Id(playerId)).some
            }
          case _ => none
        }
    }

    object Out {

      def publishState(id: RacerRace.Id, data: JsObject) = s"racer/state $id ${Json stringify data}"
    }
  }
}
