package lila.game

import lila.db.api._
import lila.common.PimpedJson._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import play.modules.reactivemongo.Implicits._

object PgnRepo {

  private implicit def inColl = pgnInColl

  def get(id: String): Fu[String] = getOption(id) map (~_)

  def getOption(id: String): Fu[Option[String]] =
    $query($select(id)).one map { _ flatMap (_ str "p") }

  def save(id: String, pgn: String): Funit =
    $update($select(id), $set("p" -> pgn), upsert = true)

  def removeIds(ids: List[String]): Funit = $remove($select byIds ids)
}
