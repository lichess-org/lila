package lila
package forum

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.DBRef
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class PostRepo(
    collection: MongoCollection) extends SalatDAO[Post, String](collection) {

  def byId(id: String): IO[Option[Post]] = io {
    findOneByID(id)
  }

  def countByTopics(topics: List[Topic]): IO[Int] = io {
    count(byTopicsQuery(topics)).toInt
  }

  def lastByTopics(topics: List[Topic]): IO[Post] = io {
    find(byTopicsQuery(topics)).sort(sortQuery).limit(1).next
  }

  val all: IO[List[Post]] = io {
    find(DBObject()).toList
  }

  val sortQuery = DBObject("createdAt" -> -1)

  def byTopicQuery(topic: Topic) = DBObject("topicId" -> topic.id)

  private def byTopicsQuery(topics: List[Topic]) =
    "topicId" $in topics.map(_.id)
}
