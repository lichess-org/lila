package lila.racer

import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

import lila.common.Json._
import lila.common.LightUser
import lila.common.LightUser.lightUserWrites
import lila.storm
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
      "players" -> race.players.zipWithIndex.map { case (player, index) =>
        Json
          .obj("index" -> (index + 1), "score" -> player.score)
          .add("user" -> player.userId.flatMap(lightUserSync))
      }
    )
}
