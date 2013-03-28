package lila.user

import lila.db.Types._
import lila.db.api._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import play.modules.reactivemongo.Implicits._

import org.joda.time.DateTime

final class HistoryRepo(implicit coll: Coll) {

  def addEntry(userId: String, elo: Int, opponentElo: Option[Int]): Funit =
    coll.update(
      select(userId),
      op.$push("entries", opponentElo.fold(Json.arr(DateTime.now.getSeconds.toInt, elo)) { opElo ⇒
        Json.arr(DateTime.now.getSeconds.toInt, elo, opElo)
      }),
      upsert = true
    ).void

  def userElos(userId: String): Fu[Seq[(Int, Int, Option[Int])]] =
    coll.find(select(userId)).one[JsObject] map { historyOption ⇒
      ~(for {
        history ← historyOption
        entries ← (history \ "entries").asOpt[JsArray]
        arrays = entries.value.map(_.asOpt[JsArray]).flatten
        elems = arrays map { array ⇒
          for {
            ts ← array(0).asOpt[Int]
            elo ← array(1).asOpt[Int]
            op = array(2).asOpt[Int]
          } yield (ts, elo, op)
        }
      } yield elems.flatten sortBy (_._1))
    }

  // def fixAll = io {
      // import java.lang.Float.parseFloat
      // import java.lang.Integer.parseInt
  //   collection.find() foreach { history ⇒
  //     val initEntries = history.as[MongoDBList]("entries").toList
  //     val entries = (initEntries collect {
  //       case elem: com.mongodb.BasicDBList ⇒ try {
  //         for {
  //           ts ← elem.getAs[Double](0)
  //           elo ← elem.getAs[Double](1)
  //           op = if (elem.size > 2) elem.getAs[Double](2) else None
  //         } yield op.fold(List(ts.toInt, elo.toInt)) { o ⇒
  //           List(ts.toInt, elo.toInt, o.toInt)
  //         }
  //       }
  //       catch {
  //         case (err: Exception) ⇒
  //           for {
  //             ts ← elem.getAs[Int](0)
  //             elo ← elem.getAs[Int](1)
  //             op = if (elem.size > 2) elem.getAs[Int](2) else None
  //           } yield op.fold(List(ts, elo)) { o ⇒ List(ts, elo, o) }
  //       }
  //     }).flatten sortBy (_.head)
  //     val id = history.as[String]("_id")
  //     println("%s: %d -> %d".format(id, initEntries.size, entries.size))
  //     collection.update(
  //       DBObject("_id" -> id),
  //       $set(Seq("entries" -> entries))
  //     )
  //   }
  // }
}
