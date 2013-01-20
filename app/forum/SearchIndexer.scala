package lila
package forum

import search.{ ElasticSearch, TypeIndexer }

import scalaz.effects._
import com.codahale.jerkson.Json

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query._, QueryBuilders._

import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.mongodb.casbah.query.Imports._

private[forum] final class SearchIndexer(
  es: EsIndexer, 
  postApi: PostApi,
  postRepo: PostRepo) {

  val indexName = "lila"
  val typeName = "post"

  private val indexer = new TypeIndexer(es, typeName, SearchMapping.mapping, indexQuery)

  val rebuildAll = indexer.rebuildAll

  val optimize = indexer.optimize

  val search = indexer.search _

  val count = indexer.count _

  def insertOne(post: Post) = for {
    viewOption ← postApi liteView post
    _ ← ~viewOption.map(view ⇒ SearchMapping(view) match {
      case (id, doc) ⇒ indexer.insertOne(id, doc)
    })
  } yield ()

  def removeOne(post: Post) = indexer removeOne post.id

  def removeTopic(topic: Topic) = io {
    es.deleteByQuery(
      Seq(indexName), 
      Seq(typeName), 
      termQuery(SearchMapping.fields.topicId, topic.id)
    )
  }

  private def indexQuery(query: DBObject) {
    val cursor = postRepo find query
    for (posts ← cursor grouped 500) {
      val views = (postApi liteViews posts.toList).unsafePerformIO
      indexer.insertMany(views.map(SearchMapping.apply).toMap).unsafePerformIO
    }
    cursor.count
  }

  def toViews(response: SearchResponse): IO[List[PostView]] = postApi viewsFromIds {
    response.hits.hits.toList map (_.id)
  }
}
