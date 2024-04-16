package lila.storm

import play.api.libs.json.*

import lila.core.pref.Pref

final class StormJson(sign: StormSign):

  import StormJson.given

  def apply(puzzles: List[StormPuzzle], user: Option[User], pref: Option[Pref]): JsObject =
    Json
      .obj("puzzles" -> puzzles)
      .add("pref" -> pref)
      .add("key" -> user.map(sign.getPrev))

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

  import lila.common.Json.given

  given OWrites[StormHigh] = Json.writes

  private val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("Y/M/d")

  given Writes[StormDay.Id] = Writes { id =>
    JsString(dateFormatter.print(id.day.toDate))
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

  given Writes[lila.core.pref.Pref] = Writes { p =>
    Json.obj(
      "coords"      -> p.coords,
      "rookCastle"  -> p.rookCastle,
      "destination" -> p.destination,
      "moveEvent"   -> p.moveEvent,
      "highlight"   -> p.highlight,
      "is3d"        -> p.is3d,
      "animation"   -> p.animationMillisForSpeedPuzzles,
      "ratings"     -> p.showRatings
    )
  }
