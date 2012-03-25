package lila.system
package db

import model.Message

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

abstract class TimelineRepo[A <: AnyRef](collection: MongoCollection, max: Int)(implicit m: Manifest[A]) extends SalatDAO[A, Int](collection) {

  val idSelector = DBObject("_id" -> true)
  val idSorter = DBObject("_id" -> -1)

  val lastId: () ⇒ IO[Option[Int]] = () ⇒ io {
    collection.find(DBObject(), idSelector)
      .sort(idSorter)
      .limit(1)
      .next()
      .getAs[Int]("_id")
  }

  val recent = io {
    find(DBObject()).sort(idSorter).limit(max).toList
  }

  def since(id: Int): IO[List[A]] = io {
    find("_id" $gt id).sort(idSorter).toList
  }
}
