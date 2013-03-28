package lila

import lila.db.{ Tube, InColl }
import Tube.Helpers._
import play.api.libs.json._

package object user extends PackageObject with WithPlay {

  private val userDefaults = Json.obj(
    "isChatBan" -> false,
    "settings" -> Json.obj(),
    "engine" -> false,
    "toints" -> 0)

  lazy val userTube = Tube[User](
    reader = (__.json update (
      merge(userDefaults) andThen readDate('createdAt)
    )) andThen Json.reads[User],
    writer = Json.writes[User],
    writeTransformer = (__.json update writeDate('createdAt)).some
  ) inColl Env.current.userColl

  private[user] sealed trait History

  lazy val historyInColl = InColl[History](Env.current.historyColl)
}
