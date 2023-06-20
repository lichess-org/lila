package lila.racer

import play.api.libs.json.*

import lila.common.Json.given
import lila.storm.StormJson
import lila.pref.Pref

final class RacerJson:

  import StormJson.given

  given OWrites[RacerPlayer] = OWrites { p =>
    Json
      .obj("name" -> p.name, "score" -> p.score)
      .add("userId", p.user.map(_.id))
      .add("title", p.user.flatMap(_.title))
  }

  // full race data
  def data(race: RacerRace, player: RacerPlayer, pref: Pref) =
    Json.obj(
      "data" -> {
        Json
          .obj(
            "race" -> Json
              .obj("id" -> race.id.value)
              .add("lobby", race.isLobby),
            "player"  -> player,
            "puzzles" -> race.puzzles
          )
          .add("owner", race.owner == player.id) ++
          state(race)
      },
      "pref" -> pref
    )

  // socket updates
  def state(race: RacerRace) = Json
    .obj("players" -> race.players)
    .add("startsIn", race.startsInMillis)
