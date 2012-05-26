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

  def countByTopics(topics: List[Topic]): IO[Int] = io {
    count(topicsQuery(topics)).toInt
  }

  def lastByTopics(topics: List[Topic]): IO[Post] = io {
    find(topicsQuery(topics)).sort(sortQuery).limit(1).next
  }

  val all: IO[List[Post]] = io {
    find(DBObject()).toList
  }

  private val sortQuery = DBObject("createdAt" -> -1)

  private def topicsQuery(topics: List[Topic]) =
    "topicId" $in topics.map(_.id)
}
