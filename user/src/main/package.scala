package lila

import lila.db.Tube
import Tube.Helpers._
import play.api.libs.json._

package object user extends PackageObject with WithPlay {

  private val userDefaults = Json.obj(
    "isChatBan" -> false,
    "settings" -> Json.obj(),
    "engine" -> false,
    "toints" -> 0)

  implicit lazy val userTube = Tube(
    reader = (__.json update (
      merge(userDefaults) andThen readDate('createdAt)
    )) andThen Json.reads[User],
    writer = Json.writes[User],
    writeTransformer = (__.json update writeDate('createdAt)).some
  )(Env.current.userRepo.coll)
}
