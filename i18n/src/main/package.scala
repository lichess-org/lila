package lila

import lila.db.Tube
import Tube.Helpers._
import play.api.libs.json._

package object i18n extends PackageObject with WithPlay {

  private val defaults = Json.obj(
    "author" -> none[String],
    "comment" -> none[String])

  private[i18n] val translationTube = Tube(
    reader = (__.json update (
      merge(defaults) andThen readDate('createdAt)
    )) andThen Json.reads[Translation],
    writer = Json.writes[Translation],
    writeTransformer = (__.json update (
      writeDate('createdAt)
    )).some
  ) inColl Env.current.translationColl
}
