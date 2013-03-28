package lila

import lila.db.Tube
import play.api.libs.json._

package object wiki extends PackageObject with WithPlay {

  implicit val pageTube = Tube(Json.reads[Page], Json.writes[Page])
}
