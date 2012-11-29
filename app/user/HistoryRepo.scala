package lila
package user

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._

import java.lang.Float.parseFloat
import java.lang.Integer.parseInt
import scalaz.effects._

final class HistoryRepo(collection: MongoCollection) {

  import HistoryRepo._

  def addEntry(
    username: String,
    elo: Int,
    gameId: Option[String] = None,
    entryType: Int = TYPE_GAME): IO[Unit] = io {
    collection.update(
      DBObject("_id" -> username.toLowerCase),
      $set(Seq(("entries." + nowSeconds) -> DBObject(
        "t" -> entryType,
        "e" -> elo,
        "g" -> (gameId | null)
      ))),
      multi = false, upsert = true
    )
  }

  def userElos(username: String): IO[List[(Int, Int)]] = io {
    ~(collection.findOne(
      DBObject("_id" -> username.toLowerCase)
    ).map(history ⇒ (for {
        (ts, v) ← history.as[DBObject]("entries")
        elo = v.asInstanceOf[DBObject]("e").toString
      } yield parseInt(ts) -> parseFloat(elo).toInt).toList))
  } map (_ sortBy (_._1)) except { err ⇒
    putStrLn(err.getMessage) inject Nil
  }
}

object HistoryRepo {

  val TYPE_START = 1;
  val TYPE_GAME = 2;
  val TYPE_ADJUST = 3;
}
