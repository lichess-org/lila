package lila.system
package db

import model.Entry

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

class EntryRepo(collection: MongoCollection)
    extends SalatDAO[Entry, String](collection) {

  private val idSelector = DBObject("_id" -> true)
  private val idSorter = DBObject("_id" -> -1)

  val lastId: () ⇒ IO[Option[Int]] = () ⇒ io {
    collection.find(DBObject(), idSelector)
      .sort(idSorter)
      .limit(1)
      .next()
      .getAs[Int]("_id")
  }

  def recent(max: Int) = io {
    find(DBObject()).sort(idSorter).limit(max).toList
  }

  def since(id: Int): IO[List[Entry]] = io {
    find("_id" $gt id).sort(idSorter).toList
  }
}
