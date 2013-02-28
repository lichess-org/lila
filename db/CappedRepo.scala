package lila.db

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

abstract class CappedRepo[A](collection: MongoCollection, max: Int) {

  val naturalOrder = DBObject("$natural" -> -1)

  val recent: IO[List[A]] = io {
    collection.find(DBObject())
      .sort(naturalOrder)
      .limit(max)
      .toList.map(decode).flatten
  }

  def decode(obj: DBObject): Option[A]

  def encode(obj: A): DBObject
}
