package lila.search
package actorApi

import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.action.search.{ SearchResponse => ESSR }
import play.api.libs.json.JsObject

case object Clear
case object RebuildAll
case object Optimize

case class InsertOne(id: String, doc: JsObject)
case class InsertMany(list: Map[String, JsObject])
case class RemoveOne(id: String)

case class Search(definition: SearchDefinition)
case class SearchResponse(res: ESSR)

case class Count(definition: CountDefinition)
case class CountResponse(res: Int)
