package lila.game

import lila.db.api._
import lila.common.PimpedJson._
import tube.pgnTube

import play.api.libs.json._

object PgnRepo {

  type ID = String

  def get(id: ID): Fu[String] = getOption(id) map (~_)

  def getOption(id: ID): Fu[Option[String]] =
    $find.one($select(id)) map { _ flatMap (_ str "p") }

  def associate(ids: Seq[ID]): Fu[Map[String, String]] =
    $find($select byIds ids) map2 { (obj: JsObject) ⇒
      obj str "p" flatMap { pgn ⇒
        obj str "_id" map (_ -> pgn)
      }
    } map (_.flatten.toMap)

  def save(id: ID, pgn: String): Funit =
    $update.field(id, "p", pgn, upsert = true)

  def removeIds(ids: List[ID]): Funit = $remove($select byIds ids)
}
