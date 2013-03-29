package lila

import lila.db.Tube
import Tube.Helpers._
import play.api.libs.json._

package object forum extends PackageObject with WithPlay {

  private val categDefaults = Json.obj(
    "team" -> none[String],
    "nbTopics" -> 0,
    "nbPosts" -> 0,
    "lastPostId" -> "")

  private[forum] lazy val categTube = Tube(
    reader = (__.json update merge(categDefaults)) andThen Json.reads[Categ],
    writer = Json.writes[Categ]
  ) inColl Env.current.categColl
}
