package lila
package search

import scalaz.effects._
import com.codahale.jerkson.Json

import org.elasticsearch.action.search.SearchResponse

import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.mongodb.casbah.query.Imports._

final class TypeIndexer(
    es: EsIndexer,
    typeName: String,
    mapping: Map[String, Any]) {

  private val indexName = "lila"

  def search(request: ElasticSearch.Request.Search): SearchResponse = request.in(indexName, typeName)(es)

  def count(request: ElasticSearch.Request.Count): Int = request.in(indexName, typeName)(es)

  def rebuildAll(indexQuery: DBObject ⇒ IO[Int]): IO[Unit] = for {
    _ ← clear
    nb ← indexQuery(DBObject())
    _ ← io { es.waitTillCountAtLeast(Seq(indexName), typeName, nb) }
    _ ← optimize
  } yield ()

  val clear: IO[Unit] = io {
    es.deleteIndex(Seq(indexName))
  } inject () except { e ⇒ putStrLn("Index does not exist yet") } map { _ ⇒
    es.createIndex(indexName, settings = Map())
    es.waitTillActive()
    es.putMapping(indexName, typeName, Json generate Map(typeName -> mapping))
    es.refresh()
  }

  val optimize: IO[Unit] = io {
    es.optimize(Seq(indexName))
  }
}
