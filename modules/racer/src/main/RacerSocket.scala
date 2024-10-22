package lila.racer

import play.api.libs.json.{ JsObject, Json }

import lila.core.socket.{ protocol as P, * }
import lila.room.RoomSocket.{ Protocol as RP, * }

final private class RacerSocket(
    api: RacerApi,
    json: RacerJson,
    socketKit: SocketKit
)(using Executor):

  import RacerSocket.*

  def publishState(race: RacerRace): Unit = send.exec:
    Protocol.Out.publishState(race.id, json.state(race))

  private lazy val send: SocketSend = socketKit.send("racer-out")

  lazy val rooms = makeRoomMap(send)

  private lazy val racerHandler: SocketHandler =
    case Protocol.In.PlayerJoin(raceId, playerId) =>
      api.join(raceId, playerId)
    case Protocol.In.PlayerScore(raceId, playerId, score) =>
      api.registerPlayerScore(raceId, playerId, score)
    case Protocol.In.RaceStart(raceId, playerId) =>
      api
        .get(raceId)
        .filter(_.startsAt.isEmpty)
        .filter(_.owner == playerId)
        .foreach(api.manualStart)

  socketKit.subscribe("racer-in", Protocol.In.reader.orElse(RP.In.reader)):
    racerHandler.orElse(minRoomHandler(rooms, logger)).orElse(socketKit.baseHandler)

  api.registerSocket(this)

object RacerSocket:

  object Protocol:

    object In:

      case class PlayerJoin(race: RacerRace.Id, player: RacerPlayer.Id)              extends P.In
      case class PlayerScore(race: RacerRace.Id, player: RacerPlayer.Id, score: Int) extends P.In
      case class RaceStart(race: RacerRace.Id, player: RacerPlayer.Id)               extends P.In

      val reader: P.In.Reader =
        case P.RawMsg("racer/join", raw) =>
          raw.get(2) { case Array(raceId, playerId) =>
            PlayerJoin(RacerRace.Id(raceId), RacerPlayer.Id(playerId)).some
          }
        case P.RawMsg("racer/score", raw) =>
          raw.get(3) { case Array(raceId, playerId, scoreStr) =>
            scoreStr.toIntOption.map { PlayerScore(RacerRace.Id(raceId), RacerPlayer.Id(playerId), _) }
          }
        case P.RawMsg("racer/start", raw) =>
          raw.get(2) { case Array(raceId, playerId) =>
            RaceStart(RacerRace.Id(raceId), RacerPlayer.Id(playerId)).some
          }

    object Out:

      def publishState(id: RacerRace.Id, data: JsObject) = s"racer/state $id ${Json.stringify(data)}"
