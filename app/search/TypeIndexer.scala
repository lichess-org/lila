package lila
package search

import scalaz.effects._

import org.elasticsearch.action.search.SearchResponse

import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.mongodb.casbah.query.Imports._

import akka.actor._
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import scala.concurrent.{ Future, Promise }
import play.api.libs.concurrent._
import play.api.libs.json._
import play.api.Play.current

final class TypeIndexer(
    es: EsIndexer,
    typeName: String,
    mapping: JsObject,
    indexQuery: DBObject ⇒ Unit) {

  private val indexName = "lila"

  def search(request: ElasticSearch.Request.Search): SearchResponse = request.in(indexName, typeName)(es)

  def count(request: ElasticSearch.Request.Count): Int = request.in(indexName, typeName)(es)

  val rebuildAll: IO[Unit] = io { actor ! RebuildAll }

  val optimize: IO[Unit] = io { actor ! Optimize }

  val clear: IO[Unit] = io { actor ! Clear }

  def insertOne(id: String, doc: JsObject) = io { actor ! InsertOne(id, doc) }

  def insertMany(list: Map[String, JsObject]) = io { actor ! InsertMany(list) }

  def removeOne(id: String) = io { actor ! RemoveOne(id) }

  private case object Clear
  private case object RebuildAll
  private case object Optimize
  private case class InsertOne(id: String, doc: JsObject)
  private case class InsertMany(list: Map[String, JsObject])
  private case class RemoveOne(id: String)

  private lazy val actor = Akka.system.actorOf(Props(new Actor {

    def receive = {

      case Clear ⇒ doClear

      case RebuildAll ⇒ {
        doClear
        indexQuery(DBObject())
      }

      case Optimize           ⇒ es.optimize(Seq(indexName))

      case InsertOne(id, doc) ⇒ es.index(indexName, typeName, id, Json stringify doc)

      case InsertMany(list) ⇒ es bulk {
        list map {
          case (id, doc) ⇒ es.index_prepare(indexName, typeName, id, Json stringify doc).request
        }
      }

      case RemoveOne(id) ⇒ es.delete(indexName, typeName, id)
    }

    private def doClear {
      try {
        es.createIndex(indexName, settings = Map())
      }
      catch {
        case e: org.elasticsearch.indices.IndexAlreadyExistsException ⇒
      }
      try {
        es.deleteByQuery(Seq(indexName), Seq(typeName))
        es.waitTillActive()
        es.deleteMapping(indexName :: Nil, typeName.some)
      }
      catch {
        case e: org.elasticsearch.indices.TypeMissingException ⇒
      }
      es.putMapping(indexName, typeName, Json stringify Json.obj(typeName -> mapping))
      es.refresh()
    }
  }))
}
