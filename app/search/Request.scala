package lila
package search

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.ActionRequest
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._
import org.elasticsearch.search._, facet._, terms._, sort._, SortBuilders._, builder._

import com.traackr.scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.traackr.scalastic.elasticsearch.SearchParameterTypes

import Game.fields._

case class Request(
    query: QueryBuilder,
    size: Int = 10,
    from: Int = 0,
    filter: Option[FilterBuilder] = None,
    sortings: Iterable[SearchParameterTypes.Sorting] = Nil) {

  val explain = true

  def in(indexName: String, typeName: String)(es: EsIndexer) =
    es.search(Seq(indexName), Seq(typeName), query,
      filter = filter,
      sortings = sortings,
      from = from.some,
      size = size.some,
      explain = explain.some
    ) ~ { response ⇒
        println("Took " + response.tookInMillis + "ms")
        println("Total hits " + response.hits.totalHits)
      }
}
