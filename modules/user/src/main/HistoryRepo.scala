package lila.user

import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.db.api._
import lila.db.InColl
import lila.db.Types._
import tube.historyTube

object HistoryRepo {

  def addEntry(userId: String, elo: Int, opponentElo: Option[Int]): Funit =
    $update(
      $select(userId),
      $push("entries", opponentElo.fold(Json.arr(DateTime.now.getSeconds.toInt, elo)) { opElo ⇒
        Json.arr(DateTime.now.getSeconds.toInt, elo, opElo)
      }),
      upsert = true)

  def userElos(userId: String): Fu[Seq[(Int, Int, Option[Int])]] =
    $find.one($select(userId)) map { historyOption ⇒
      ~(for {
        history ← historyOption
        entries ← (history \ "entries").asOpt[JsArray]
        arrays = entries.value.map(_.asOpt[JsArray])
        elems = arrays collect {
          case Some(array) ⇒ (
            array(0).asOpt[Int].getOrElse(0),
            array(1).asOpt[Int].getOrElse(0),
            array(2).asOpt[Int])
        }
      } yield elems)
    }
}
