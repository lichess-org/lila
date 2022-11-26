package lila.storm

import play.api.libs.json.*

import lila.user.User
import org.joda.time.format.DateTimeFormat

final class StormJson(sign: StormSign):

  import StormJson.given

  def apply(puzzles: List[StormPuzzle], user: Option[User]): JsObject = Json
    .obj(
      "puzzles"      -> puzzles,
      "notAnExploit" -> StormForm.notAnExploit
    )
    .add("key" -> user.map(sign.getPrev))

  def pref(p: lila.pref.Pref) =
    Json.obj(
      "coords"      -> p.coords,
      "rookCastle"  -> p.rookCastle,
      "destination" -> p.destination,
      "moveEvent"   -> p.moveEvent,
      "highlight"   -> p.highlight,
      "is3d"        -> p.is3d,
      "animation"   -> p.animationMillisForSpeedPuzzles
    )

  def newHigh(n: Option[StormHigh.NewHigh]) =
    Json
      .obj()
      .add("newHigh" -> n.map { nh =>
        Json.obj(
          "key"  -> nh.key,
          "prev" -> nh.previous
        )
      })

  def apiDashboard(high: StormHigh, days: List[StormDay]) = Json.obj(
    "high" -> high,
    "days" -> days
  )

object StormJson:

  import lila.puzzle.JsonView.given
  import lila.common.Json.given

  given OWrites[StormHigh] = Json.writes

  private val dateFormat = DateTimeFormat forPattern "Y/M/d"

  given Writes[StormDay.Id] = Writes { id =>
    JsString(dateFormat print id.day.toDate)
  }
  given OWrites[StormDay] = Json.writes

  given OWrites[StormPuzzle] = OWrites { p =>
    Json.obj(
      "id"     -> p.id,
      "fen"    -> p.fen.value,
      "line"   -> p.line.toList.map(_.uci).mkString(" "),
      "rating" -> p.rating
    )
  }
