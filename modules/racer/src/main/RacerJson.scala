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

  // implicit val playerIdWrites: OWrites[RacerPlayer.Id] = OWrites {
  //   case RacerPlayer.Id.Anon(

  def raceJson(race: RacerRace, me: Either[String, User]) =
    Json.obj(
      "id" -> race.id.value,
      "owner" -> {
        (race.owner, me) match {
          case (RacerPlayer.Id.User(id), Right(u)) => id == u.id
          case (RacerPlayer.Id.Anon(a), Left(b))   => a == b
          case _                                   => false
        }
      },
      "players" -> race.players.zipWithIndex.map { case (player, index) =>
        Json
          .obj("index" -> index)
          .add("user" -> player.userId.flatMap(lightUserSync))
      }
    )

  // def apply(race: RacerRace, puzzles: List[StormPuzzle], user: Option[User]): JsObject = Json
  //   .obj(
  //     "race"    -> race,
  //     "puzzles" -> puzzles
  //   )
  //   .add("key" -> user.map(sign.getPrev))
}
