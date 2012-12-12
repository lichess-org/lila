package lila
package forum

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class CategRepo(
    collection: MongoCollection) extends SalatDAO[Categ, String](collection) {

  def bySlug(slug: String): IO[Option[Categ]] = io {
    findOneById(slug)
  }

  val all: IO[List[Categ]] = io {
    find(DBObject()).sort(DBObject("pos" -> 1)).toList
  }

  def saveIO(categ: Categ): IO[Unit] = io {
    update(
      DBObject("_id" -> categ.slug),
      _grater asDBObject categ,
      upsert = true)
  }

  val nextPosition: IO[Int] = io {
    (collection.find(DBObject(), DBObject("pos" -> true))
      .sort(DBObject("pos" -> -1))
      .limit(1) map { obj ⇒
        obj.getAs[Int]("pos")
      }).flatten.toList.headOption | 0
  } map (_ + 1)

  def nbPosts(id: String): IO[Int] = io {
    primitiveProjection[Int](idSelector(id), "nbPosts")
  } map (_ | 0)

  private def idSelector(categ: Categ) = DBObject("_id" -> categ.slug)

  private def idSelector(slug: String) = DBObject("_id" -> slug)
}
