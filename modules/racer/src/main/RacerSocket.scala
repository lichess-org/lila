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
      api.join(raceId, playerId).unit
    case Protocol.In.PlayerScore(raceId, playerId, score) =>
      api.registerPlayerScore(raceId, playerId, score)
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
      case class PlayerScore(race: RacerRace.Id, player: RacerPlayer.Id, score: Int) extends P.In

      val reader: P.In.Reader = raw => raceReader(raw) orElse RP.In.reader(raw)

      val raceReader: P.In.Reader = raw =>
        raw.path match {
          case "racer/join" =>
            raw.get(2) { case Array(raceId, playerId) =>
              PlayerJoin(RacerRace.Id(raceId), RacerPlayer.Id(playerId)).some
            }
          case "racer/score" =>
            raw.get(3) { case Array(raceId, playerId, scoreStr) =>
              scoreStr.toIntOption map { PlayerScore(RacerRace.Id(raceId), RacerPlayer.Id(playerId), _) }
            }
          case _ => none
        }
    }

    object Out {

      def publishState(id: RacerRace.Id, data: JsObject) = s"racer/state $id ${Json stringify data}"
    }
  }
}
