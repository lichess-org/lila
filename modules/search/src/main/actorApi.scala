package lila.search
package actorApi

import com.sksamuel.elastic4s.{ CountDefinition, SearchDefinition }
import org.elasticsearch.action.search.{ SearchResponse => ESSR }

case object Reset

case class Search(definition: SearchDefinition)
case class SearchResponse(res: ESSR)

case class Count(definition: CountDefinition)
case class CountResponse(res: Int)
