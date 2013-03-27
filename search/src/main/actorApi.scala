package lila.search
package actorApi

import play.api.libs.json.JsObject
import org.elasticsearch.action.search.{ SearchResponse ⇒ ESSR }

case object Clear
case object RebuildAll
case object Optimize

case class InsertOne(id: String, doc: JsObject)
case class InsertMany(list: Map[String, JsObject])
case class RemoveOne(id: String)

case class Search(request: ElasticSearch.Request.Search)
case class SearchResponse(res: ESSR)
case class Count(request: ElasticSearch.Request.Count)
case class CountResponse(res: Int)
