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

  def byId(id: String): IO[Option[Topic]] = io {
    findOneByID(id)
  }

  def byCateg(categ: Categ): IO[List[Topic]] = io {
    find(DBObject("categId" -> categ.slug)).toList
  }

  def byTree(categSlug: String, slug: String): IO[Option[Topic]] = io {
    findOne(DBObject(
      "categId" -> categSlug,
      "slug" -> slug
    ))
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

  val sortQuery = DBObject("createdAt" -> -1)

  def byCategQuery(categ: Categ) = DBObject("categId" -> categ.slug)
}
