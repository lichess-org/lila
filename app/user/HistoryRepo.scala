package lila
package user

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._

import java.lang.Float.parseFloat
import java.lang.Integer.parseInt
import scalaz.effects._
import org.joda.time.DateTime

final class HistoryRepo(collection: MongoCollection) {

  def addEntry(userId: String, elo: Int, opponentElo: Option[Int]): IO[Unit] = io {
    collection.update(
      DBObject("_id" -> userId),
      $push("entries" -> opponentElo.fold(
        DBList(DateTime.now.getSeconds.toInt, elo, _),
        DBList(DateTime.now.getSeconds.toInt, elo))
      ),
      multi = false,
      upsert = true)
  }

  def userElos(username: String): IO[List[(Int, Int, Option[Int])]] = io {
    ~collection.findOne(
      DBObject("_id" -> username.toLowerCase)
    ).map(history ⇒
        (history.as[MongoDBList]("entries").toList collect {
          case elem: com.mongodb.BasicDBList ⇒ try {
            for {
              ts ← elem.getAs[Double](0)
              elo ← elem.getAs[Double](1)
              op = if (elem.size > 2) elem.getAs[Double](2) else None
            } yield (ts.toInt, elo.toInt, op map (_.toInt))
          }
          catch {
            case (err: Exception) ⇒ {
              println("ERR while parsing %s history: %s(%s)".format(username, err.getClass, err.getMessage))
              none
            }
          }
        }).flatten sortBy (_._1)
      )
  }
}
