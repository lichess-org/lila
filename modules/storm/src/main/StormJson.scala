package lila.storm

import play.api.libs.json._

import lila.common.Json._
import lila.user.User

final class StormJson(sign: StormSign) {

  import StormJson.puzzleWrites

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
      "is3d"        -> p.is3d
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
}

object StormJson {

  import lila.puzzle.JsonView.puzzleIdWrites

  implicit val puzzleWrites: OWrites[StormPuzzle] = OWrites { p =>
    Json.obj(
      "id"     -> p.id,
      "fen"    -> p.fen.value,
      "line"   -> p.line.toList.map(_.uci).mkString(" "),
      "rating" -> p.rating
    )
  }
}
