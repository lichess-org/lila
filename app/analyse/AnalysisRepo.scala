package lila
package analyse

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._

import scalaz.effects._
import org.joda.time.DateTime

final class AnalysisRepo(collection: MongoCollection) {

  def done(id: String, a: Analysis) = io {
    collection.update(
      DBObject("_id" -> id),
      $set(
        "done" -> true,
        "encoded" -> a.encode)
    )
  }

  def progress(id: String, userId: String) = io {
    collection.insert(DBObject(
      "_id" -> id,
      "uid" -> userId,
      "done" -> false,
      "date" -> DateTime.now))
  }

  def byId(id: String): IO[Option[Analysis]] = io {
    for {
      obj ← collection.findOne(DBObject("_id" -> id))
      done = obj.getAs[Boolean]("done") | false
      infos = for {
        encoded ← obj.getAs[String]("encoded")
        decoded ← (Analysis decode encoded).toOption
      } yield decoded
    } yield Analysis(infos | Nil, done)
  }
}
