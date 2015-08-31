package lila.search

import scala.concurrent.Future

import com.sksamuel.elastic4s.ElasticDsl
import com.sksamuel.elastic4s.ElasticDsl.{ RichFuture => _, _ }
import com.sksamuel.elastic4s.Executable
import com.sksamuel.elastic4s.mappings.{ PutMappingDefinition }
import com.sksamuel.elastic4s.{ ElasticClient, CountDefinition, SearchDefinition, IndexDefinition, BulkDefinition }

sealed trait ESClient {

  def search(d: SearchDefinition): Fu[SearchResponse]
  def count(d: CountDefinition): Fu[CountResponse]

  def store(d: IndexDefinition): Funit
  def deleteById(id: String, indexType: String): Funit
  def deleteByQuery(query: String, indexType: String): Funit
  def bulk(d: BulkDefinition): Funit

  def put(d: PutMappingDefinition): Funit

  // synchronously create index and type, ignoring errors (already existing)
  def createType(indexName: String, typeName: String): Unit
}

object ESClient {

  def make(client: Option[ElasticClient]): ESClient = client match {
    case None    => new StubESClient
    case Some(c) => new RealESClient(c)
  }
}

final class StubESClient extends ESClient {

  def search(d: SearchDefinition) = fuccess(SearchResponse.stub)
  def count(d: CountDefinition) = fuccess(CountResponse.stub)

  def store(d: IndexDefinition) = funit
  def deleteById(id: String, indexType: String) = funit
  def deleteByQuery(query: String, indexType: String) = funit
  def bulk(d: BulkDefinition) = funit

  def put(d: PutMappingDefinition) = funit

  def createType(indexName: String, typeName: String) {
  }
}

final class RealESClient(client: ElasticClient) extends ESClient {

  def search(d: SearchDefinition) = client execute d map SearchResponse.apply
  def count(d: CountDefinition) = client execute d map CountResponse.apply

  def store(d: IndexDefinition) = client execute d void
  def deleteById(id: String, indexType: String) = client execute {
    ElasticDsl.delete id id from indexType
  } void
  def deleteByQuery(query: String, indexType: String) = client execute {
    ElasticDsl.delete from indexType where query
  } void
  def bulk(d: BulkDefinition) = client execute d void

  def put(d: PutMappingDefinition) = client execute d void

  def createType(indexName: String, typeName: String) {
    try {
      import scala.concurrent.Await
      import scala.concurrent.duration._
      Await.result(client execute {
        create index indexName
      }, 10.seconds)
    }
    catch {
      case e: Exception => // println("create type: " + e)
    }
    // client.sync execute {
    //   delete from indexName -> typeName where matchall
    // }
    import org.elasticsearch.index.query.QueryBuilders._
    client.java.prepareDeleteByQuery(indexName)
      .setTypes(typeName)
      .setQuery(matchAllQuery)
      .execute()
      .actionGet()
  }
}
