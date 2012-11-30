package lila
package mongodb

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

import scala.reflect.{ClassTag, classTag}

final class Cache(collection: MongoCollection) {

  private val field = "v"

  def set(key: String, value: Any): Unit = {
    collection += DBObject("_id" -> key, field -> value)
  }

  def get(key: String): Option[Any] = for {
    o ← collection.findOne(select(key))
    v ← Option(o get field)
  } yield v

  def getAs[T : ClassTag](key: String): Option[T] = for {
    v ← get(key)
  } yield v.asInstanceOf[T]

  def remove(key: String) {
    collection remove select(key)
  }

  private def select(key: String) = DBObject("_id" -> key)
}
