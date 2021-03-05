package lila.racer

import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

import lila.common.Json._
import lila.common.LightUser
import lila.storm.StormJson
import lila.storm.StormPuzzle
import lila.storm.StormSign
import lila.user.User

final class RacerJson(stormJson: StormJson, sign: StormSign, lightUserSync: LightUser.GetterSync) {

  import StormJson._

  def raceJson(race: RacerRace, playerId: RacerPlayer.Id) =
    Json.obj(
      "race" -> Json.obj(
        "id"       -> race.id.value,
        "isPlayer" -> race.has(playerId),
        "isOwner"  -> (race.owner == playerId)
      ),
      "puzzles" -> race.puzzles,
      "players" -> playersJson(race)
    )

  def state(race: RacerRace) = Json.obj(
    "players" -> playersJson(race)
  )

  private def playersJson(race: RacerRace) = JsArray {
    race.players.map { case player =>
      val user = player.userId flatMap lightUserSync
      Json
        .obj("name" -> player.name, "moves" -> player.moves)
        .add("userId" -> player.userId)
        .add("title" -> user.map(_.title))
    }
  }
}
