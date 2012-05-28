package lila
package wiki

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class PageRepo(
    collection: MongoCollection) extends SalatDAO[Page, String](collection) {

  def byId(id: String): IO[Option[Page]] = io {
    findOneByID(id)
  }

  val all: IO[List[Page]] = io {
    find(DBObject()).sort(DBObject("name" -> 1)).toList
  }
}
