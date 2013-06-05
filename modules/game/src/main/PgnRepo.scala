package lila.game

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import tube.pgnTube

object PgnRepo {

  type ID = String

  def get(id: ID): Fu[String] = getOption(id) map (~_)

  def getNonEmpty(id: ID): Fu[Option[String]] =
    getOption(id) map (_ filter (_.nonEmpty))

  def getOption(id: ID): Fu[Option[String]] =
    $find.one($select(id)) map { _ flatMap (_ str "p") }

  def associate(ids: Seq[ID]): Fu[Map[String, String]] =
    $find($select byIds ids) map2 { (obj: JsObject) ⇒
      obj str "p" flatMap { pgn ⇒
        obj str "_id" map (_ -> pgn)
      }
    } map (_.flatten.toMap)

  def getOneRandom(distrib: Int): Fu[Option[String]] =
    $find($query.all skip scala.util.Random.nextInt(distrib), 1) map { 
      _.headOption flatMap (_ str "p") 
    }

  def save(id: ID, pgn: String): Funit =
    $update.field(id, "p", pgn, upsert = true)

  def removeIds(ids: List[ID]): Funit = $remove($select byIds ids)
}
