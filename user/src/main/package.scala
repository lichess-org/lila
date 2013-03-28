package lila

import lila.db.Tube
import Tube.Helpers._
import play.api.libs.json._

package object user extends PackageObject with WithPlay {

  private val defaults = Json.obj(
    "isChatBan" -> false,
    "settings" -> Json.obj(),
    "engine" -> false,
    "toints" -> 0)

  implicit val userTube = Tube(
    reader = (__.json update (
      merge(defaults) andThen readDate('createdAt)
    )) andThen Json.reads[User],
    writer = Json.writes[User],
    writeTransformer = (__.json update writeDate('createdAt)).some
  )
}
