package lila.search
package actorApi

import org.elasticsearch.action.search.{ SearchResponse => ESSR }

case object Reset

case class Search(definition: FreeSearchDefinition)
case class SearchResponse(res: ESSR)

case class Count(definition: FreeCountDefinition)
case class CountResponse(res: Int)
