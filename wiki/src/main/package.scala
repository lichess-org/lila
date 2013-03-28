package lila

import lila.db._
import play.api.libs.json._

package object wiki extends PackageObject with WithPlay {

  implicit lazy val pageTube: Tube[Page] = new Tube(
    Json.reads[Page],
    Json.writes[Page]) with InColl {
    def coll = Env.current.pageColl
  }

  implicit lazy val pageInColl: InColl[Page] = new InColl[Page] {
    def coll = Env.current.pageColl
  }
}
