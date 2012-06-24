package lila
package analyse

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

import scalaz.effects._
import org.joda.time.DateTime

final class AnalysisRepo(collection: MongoCollection) {

  def add(id: String, a: Analysis) = io {
    collection.insert(
      DBObject("_id" -> id, "encoded" -> a.encode, "date" -> DateTime.now)
    )
  }

  def byId(id: String): IO[Option[Analysis]] = io {
    for {
      obj ← collection.findOne(DBObject("_id" -> id))
      encoded ← obj.getAs[String]("encoded")
      decoded ← (Analysis decode encoded).toOption
    } yield decoded
  }
}
