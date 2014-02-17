package lila.search

import akka.actor._
import akka.pattern.pipe
import play.api.libs.json._
import scalastic.elasticsearch.{ Indexer => EsIndexer }

import actorApi._

final class TypeIndexer(
    esIndexer: Fu[EsIndexer],
    indexName: String,
    typeName: String,
    mapping: JsObject,
    indexQuery: JsObject => Funit) extends Actor {

  def receive = {

    case Search(request) => withEs { es =>
      SearchResponse(request.in(indexName, typeName)(es))
    } pipeTo sender

    case Count(request) => withEs { es =>
      CountResponse(request.in(indexName, typeName)(es))
    } pipeTo sender

    case RebuildAll => {
      self ! Clear
      indexQuery(Json.obj()) pipeTo sender
    }

    case Optimize => withEs {
      _.optimize(Seq(indexName))
    }

    case InsertOne(id, doc) => withEs {
      _.index(indexName, typeName, id, Json stringify doc)
    }

    case InsertMany(list) => withEs { es =>
      es bulk {
        list map {
          case (id, doc) => es.index_prepare(indexName, typeName, id, Json stringify doc).request
        }
      }
    }

    case RemoveOne(id) => withEs {
      _.delete(indexName, typeName, id)
    }

    case RemoveQuery(query) => withEs {
      _.deleteByQuery(Seq(indexName), Seq(typeName), query)
    }

    case Clear => withEs { es =>
      try {
        es.createIndex(indexName, settings = Map())
      }
      catch {
        case e: org.elasticsearch.indices.IndexAlreadyExistsException =>
      }
      try {
        es.deleteByQuery(Seq(indexName), Seq(typeName))
        es.waitTillActive()
        es.deleteMapping(indexName :: Nil, typeName.some)
      }
      catch {
        case e: org.elasticsearch.indices.TypeMissingException =>
      }
      es.putMapping(indexName, typeName, Json stringify Json.obj(typeName -> mapping))
      es.refresh()
    }
  }

  private def withEs[A](f: EsIndexer => A): Fu[A] = esIndexer map f
}
