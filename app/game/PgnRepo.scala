package lila
package game

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.{ WriteConcern, MongoCollection }
import com.mongodb.casbah.query.Imports._
import scalaz.effects._

final class PgnRepo(collection: MongoCollection) {

  def get(id: String): IO[String] = io {
    collection.findOne(idSelector(id)).flatMap(_.getAs[String]("p")) | ""
  }

  def save(id: String, pgn: String): IO[Unit] = io {
    collection.update(
      idSelector(id), 
      $set("p" -> pgn), 
      upsert = true, 
      multi = false, 
      concern = WriteConcern.None)
  }

  def unsafeGet(id: String): String = 
    collection.findOne(idSelector(id)).flatMap(_.getAs[String]("p")) | ""

  private def idSelector(id: String) = DBObject("_id" -> id)
}
