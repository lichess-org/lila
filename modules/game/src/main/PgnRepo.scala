package lila.game

import chess.format.pgn.Binary
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.bson.Subtype.GenericBinarySubtype
import reactivemongo.bson.{ BSONHandler, BSONDocument, BSONBinary }

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import tube.pgnColl

object PgnRepo {

  type ID = String
  type Moves = List[String]

  def get(id: ID): Fu[Moves] = getOption(id) map (~_)

  def getNonEmpty(id: ID): Fu[Option[Moves]] =
    getOption(id) map (_ filter (_.nonEmpty))

  def getOption(id: ID): Fu[Option[Moves]] =
    pgnColl.find(
      $select(id), Json.obj("_id" -> false)
    ).one[BSONDocument] map { _ flatMap docToMoves }

  def associate(ids: Seq[ID]): Fu[Map[String, Moves]] =
    pgnColl.find($select byIds ids)
      .cursor[BSONDocument]
      .collect[List]() map2 { (obj: BSONDocument) ⇒
        docToMoves(obj) flatMap { moves ⇒
          obj.getAs[String]("_id") map (_ -> moves)
        }
      } map (_.flatten.toMap)

  def getOneRandom(distrib: Int): Fu[Moves] = {
    pgnColl.find($select.all) skip scala.util.Random.nextInt(distrib)
  }.cursor[BSONDocument].collect[List](1) map {
    _.headOption flatMap docToMoves 
  } map (~_)

  def save(id: ID, moves: Moves): Funit = lila.db.api successful {
    pgnColl.update(
      $select(id),
      BSONDocument("$set" -> BSONDocument("p" -> BSONBinaryPgnHandler.write(moves))),
      upsert = true
    )
  }

  def removeIds(ids: List[ID]): Funit = lila.db.api successful {
    pgnColl.remove($select byIds ids)
  }

  private def docToMoves(doc: BSONDocument): Option[Moves] = doc.getAs[Moves]("p")

  private object BSONBinaryPgnHandler extends BSONHandler[BSONBinary, Moves] {
    def read(x: BSONBinary) = {
      val buffer = x.value
      val bytes = new Array[Byte](buffer.readInt - 1)
      buffer.readBytes(bytes)
      buffer.readByte
      (Binary readMoves bytes.toList).get.pp
    }
    def write(x: List[String]) = BSONBinary(
      (Binary writeMoves x.pp).get.toArray,
      GenericBinarySubtype)
  }
}
