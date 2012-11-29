package lila
package analyse

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._

import scalaz.effects._
import org.joda.time.DateTime

final class AnalysisRepo(val collection: MongoCollection) {

  def done(id: String, a: Analysis) = io {
    collection.update(
      DBObject("_id" -> id),
      $set(Seq("done" -> true, "encoded" -> a.encode)) ++ $unset(Seq("fail"))
    )
  }

  def fail(id: String, err: Failures) = io {
    collection.update(
      DBObject("_id" -> id),
      $set(Seq("fail" -> err.shows))
    )
  }

  def progress(id: String, userId: String) = io {
    collection.update(
      DBObject("_id" -> id),
      DBObject(
        "_id" -> id,
        "uid" -> userId,
        "done" -> false,
        "date" -> DateTime.now
      ),
      upsert = true)
  }

  def byId(id: String): IO[Option[Analysis]] = io {
    for {
      obj ← collection.findOne(DBObject("_id" -> id))
      done = obj.getAs[Boolean]("done") | false
      fail = obj.getAs[String]("fail")
      infos = for {
        encoded ← obj.getAs[String]("encoded")
        decoded ← (Analysis decode encoded).toOption
      } yield decoded
    } yield Analysis(infos | Nil, done, fail)
  }

  def doneById(id: String): IO[Option[Analysis]] = byId(id) map { _ filter (_.done) }

  def isDone(id: String): IO[Boolean] = io {
    collection.find(DBObject("_id" -> id, "done" -> true)).limit(1).size == 1
  }

  def userInProgress(uid: String): IO[Boolean] = io {
    collection.find(
      ("fail" $exists false) ++ DBObject("uid" -> uid, "done" -> false)
    ).limit(1).size > 0
  }
}
