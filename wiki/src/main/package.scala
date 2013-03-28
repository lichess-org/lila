package lila

import lila.db._
import play.api.libs.json._

package object wiki extends PackageObject with WithPlay {

  implicit lazy val pageTube = new Tube(
    Json.reads[Page],
    Json.writes[Page]) with InColl {
      def coll = Env.current.pageColl
    }
}
