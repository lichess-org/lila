package lila
package forum

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.DBRef
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class CategRepo(
    collection: MongoCollection
  ) extends SalatDAO[Categ, String](collection) {

  def categ(slug: String): IO[Option[Categ]] = io {
    findOneByID(slug)
  }

  val all: IO[List[Categ]] = io {
    find(DBObject()).sort(DBObject("pos" -> 1)).toList
  }

  private def idSelector(categ: Categ) = DBObject("_id" -> categ.slug)

  private def idSelector(slug: String) = DBObject("_id" -> slug)
}
