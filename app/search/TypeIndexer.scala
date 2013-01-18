package lila
package search

import scalaz.effects._
import com.codahale.jerkson.Json

import org.elasticsearch.action.search.SearchResponse

import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.mongodb.casbah.query.Imports._

import akka.actor._
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import akka.dispatch.{ Future, Promise }
import play.api.libs.concurrent._
import play.api.Play.current

final class TypeIndexer(
    es: EsIndexer,
    typeName: String,
    mapping: Map[String, Any],
    indexQuery: DBObject ⇒ IO[Int]) {

  private val indexName = "lila"

  private case object Clear
  private case object RebuildAll
  private case object Optimize

  private lazy val actor = Akka.system.actorOf(Props(new Actor {

    def receive = {
      case Clear ⇒ doClear
      case RebuildAll ⇒ (for {
        _ ← io { doClear }
        nb ← indexQuery(DBObject())
        _ ← io { es.waitTillCountAtLeast(Seq(indexName), typeName, nb) }
        _ ← optimize
      } yield ()).unsafePerformIO
      case Optimize ⇒ es.optimize(Seq(indexName))
    }

    private def doClear {
      try {
        es.createIndex(indexName, settings = Map())
      }
      catch {
        case e: org.elasticsearch.indices.IndexAlreadyExistsException ⇒
      }
      es.deleteByQuery(Seq(indexName), Seq(typeName))
      es.waitTillActive()
      es.putMapping(indexName, typeName, Json generate Map(typeName -> mapping))
      es.refresh()
    }
  }))

  def search(request: ElasticSearch.Request.Search): SearchResponse = request.in(indexName, typeName)(es)

  def count(request: ElasticSearch.Request.Count): Int = request.in(indexName, typeName)(es)

  val rebuildAll: IO[Unit] = io { actor ! RebuildAll }

  val optimize: IO[Unit] = io { actor ! Optimize }

  val clear: IO[Unit] = io { actor ! Clear }
}
