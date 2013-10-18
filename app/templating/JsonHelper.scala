package lila.app
package templating

import play.api.libs.json._

trait JsonHelper {

  def toJson[A: Writes](map: Map[Int, A]): String = Json stringify {
    Json toJson {
      map mapKeys (_.toString)
    }
  }

  def toJson[A: Writes](a: A): String = Json stringify {
    Json toJson a
  }
}
