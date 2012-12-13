package lila
package forum

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class PostRepo(
    collection: MongoCollection) extends SalatDAO[Post, String](collection) {

  def byId(id: String): IO[Option[Post]] = io {
    findOneById(id)
  }

  def countByTopics(topics: List[Topic]): IO[Int] = io {
    count(byTopicsQuery(topics)).toInt
  }

  def lastByTopics(topics: List[Topic]): IO[Post] = io {
    find(byTopicsQuery(topics)).sort(sortQuery(-1)).limit(1).next
  }

  val all: IO[List[Post]] = io {
    find(DBObject()).toList
  }

  def recentInCategs(nb: Int)(categIds: List[String]): IO[List[Post]] = io {
    find("categId" $in categIds).sort(sortQuery(-1)).limit(nb).toList
  }

  val sortQuery: DBObject = sortQuery(1)

  def sortQuery(order: Int): DBObject = DBObject("createdAt" -> order)

  def saveIO(post: Post): IO[Unit] = io {
    update(
      DBObject("_id" -> post.id),
      _grater asDBObject post,
      upsert = true)
  }

  def removeIO(post: Post): IO[Unit] = io {
    remove(DBObject("_id" -> post.id))
  }

  def byTopicQuery(topic: Topic) = DBObject("topicId" -> topic.id)

  private def byTopicsQuery(topics: List[Topic]) =
    "topicId" $in topics.map(_.id)
}
