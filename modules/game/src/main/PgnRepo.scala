package lila.game

import chess.format.pgn.Binary
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.Subtype.GenericBinarySubtype
import reactivemongo.bson.{ BSONHandler, BSONDocument, BSONBinary }

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._

object PgnRepo extends PgnRepo {
  def coll = tube.pgnColl
}

trait PgnRepo {

  def coll: BSONCollection

  type ID = String
  type Moves = List[String]

  def get(id: ID): Fu[Moves] = getOption(id) map (~_)

  def getNonEmpty(id: ID): Fu[Option[Moves]] =
    getOption(id) map (_ filter (_.nonEmpty))

  def getOption(id: ID): Fu[Option[Moves]] =
    coll.find(
      $select(id), Json.obj("_id" -> false)
    ).one[BSONDocument] map { _ flatMap docToMoves }

  def associate(ids: Seq[ID]): Fu[Map[String, Moves]] =
    coll.find($select byIds ids)
      .cursor[BSONDocument]
      .collect[List]() map2 { (obj: BSONDocument) ⇒
        docToMoves(obj) flatMap { moves ⇒
          obj.getAs[String]("_id") map (_ -> moves)
        }
      } map (_.flatten.toMap)

  def getOneRandom(distrib: Int): Fu[Moves] = {
    coll.find($select.all) skip scala.util.Random.nextInt(distrib)
  }.cursor[BSONDocument].collect[List](1) map {
    _.headOption flatMap docToMoves
  } map (~_)

  def save(id: ID, moves: Moves): Funit = lila.db.api successful {
    coll.update(
      $select(id),
      BSONDocument("$set" -> BSONDocument("p" -> BSONBinaryPgnHandler.write(moves))),
      upsert = true
    )
  }

  // used in migration for bulk inserting
  def make(id: ID, moves: Moves) = {
    val binary = BSONBinaryPgnHandler.write(moves)
    // BSONBinaryPgnHandler.read(binary) 
    BSONDocument("_id" -> id, "p" -> binary)
  }

  def removeIds(ids: List[ID]): Funit = lila.db.api successful {
    coll.remove($select byIds ids)
  }

  private def docToMoves(doc: BSONDocument): Option[Moves] =
    doc.getAs[BSONBinary]("p") map BSONBinaryPgnHandler.read

  private object BSONBinaryPgnHandler extends BSONHandler[BSONBinary, Moves] {
    def read(x: BSONBinary) = {
      val remaining = x.value.readable
      val bytes = x.value.slice(remaining).readArray(remaining)
      (Binary readMoves bytes.toList).get
    }
    def write(x: List[String]) = BSONBinary(
      (Binary writeMoves x).get.toArray,
      GenericBinarySubtype)
  }
}
