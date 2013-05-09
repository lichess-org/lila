package lila.search

import actorApi._

import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }

import akka.actor._
import akka.pattern.pipe
import play.api.libs.json._

final class TypeIndexer(
    es: EsIndexer,
    indexName: String,
    typeName: String,
    mapping: JsObject,
    indexQuery: JsObject ⇒ Funit) extends Actor {

  def receive = {

    case Search(request) ⇒ sender ! SearchResponse(
      request.in(indexName, typeName)(es)
    )

    case Count(request) ⇒ sender ! CountResponse(
      request.in(indexName, typeName)(es)
    )

    case Clear ⇒ doClear

    case RebuildAll ⇒ {
      self ! Clear
      indexQuery(Json.obj()) pipeTo sender
    }

    case Optimize ⇒ es.optimize(Seq(indexName))

    case InsertOne(id, doc) ⇒ es.index(
      indexName,
      typeName,
      id,
      Json stringify doc
    )

    case InsertMany(list) ⇒ es bulk {
      list map {
        case (id, doc) ⇒ es.index_prepare(
          indexName,
          typeName,
          id,
          Json stringify doc
        ).request
      }
    }

    case RemoveOne(id) ⇒ es.delete(indexName, typeName, id)

    case RemoveQuery(query) => es.deleteByQuery(
      Seq(indexName),
      Seq(typeName),
      query
    )
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
}
