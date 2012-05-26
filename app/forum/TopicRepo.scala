package lila
package forum

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.DBRef
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class TopicRepo(
    collection: MongoCollection
  ) extends SalatDAO[Topic, String](collection) {

  def byCateg(categ: Categ): IO[List[Topic]] = io {
    find(DBObject("categId" -> categ.slug)).toList
  }

  val all: IO[List[Topic]] = io {
    find(DBObject()).toList
  }

  def saveIO(topic: Topic): IO[Unit] = io {
    update(
      DBObject("_id" -> topic.id),
      _grater asDBObject topic,
      upsert = true)
  }
}
