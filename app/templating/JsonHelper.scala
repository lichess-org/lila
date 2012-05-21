package lila
package templating

import com.codahale.jerkson.Json

trait JsonHelper {

  def mapToJson(map: Map[String, Any]) = Json generate map

  def listToJson(list: List[Any]) = Json generate list
}
