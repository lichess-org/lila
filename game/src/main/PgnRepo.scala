package lila.game

import lila.db.api._
import lila.common.PimpedJson._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import play.modules.reactivemongo.Implicits._

object PgnRepo {

  type ID = String

  private implicit def inColl = pgnInColl

  def get(id: ID): Fu[String] = getOption(id) map (~_)

  def getOption(id: ID): Fu[Option[String]] =
    $query($select(id)).one map { _ flatMap (_ str "p") }

  def associate(ids: Seq[ID]): Fu[Map[String, String]] =
    $query($select byIds ids).cursor.toList map2 { (obj: JsObject) ⇒
      obj str "p" flatMap { pgn =>
        obj str "_id" map (_ -> pgn)
      }
    } map (_.flatten.toMap)

  def save(id: ID, pgn: String): Funit =
    $update($select(id), $set("p" -> pgn), upsert = true)

  def removeIds(ids: List[ID]): Funit = $remove($select byIds ids)
}
