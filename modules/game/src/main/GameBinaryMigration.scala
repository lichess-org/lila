package lila.game

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import chess.{ Pos, Castles, History }
import chess.Pos.posAt
import play.api.libs.iteratee._
import reactivemongo.bson._

import lila.db.Implicits._

object GameBinaryMigration {

  type Doc = BSONDocument
  type Docs = Iterator[Doc]

  def apply(db: lila.db.Env, system: akka.actor.ActorSystem) = {

    val oldGameColl = db("game4")
    val repo = GameRepo
    val batchSize = 100
    val limit = 150 * 1000
    // val limit = 20 * 1000 * 1000
    
    def getAs[T](map: Map[String, BSONValue], key: String)(implicit reader: BSONReader[_ <: BSONValue, T]): Option[T] = {
      map.get(key) flatMap { e => Try(reader.asInstanceOf[BSONReader[BSONValue, T]] read e).toOption }
    }

    def parseLastMove(lastMove: String): Option[(Pos, Pos)] = lastMove match {
      case History.MoveString(a, b) ⇒ for (o ← posAt(a); d ← posAt(b)) yield (o, d)
      case _ ⇒ None
    }

    def convert(o: Doc): Doc = {
      val d1 = (o.stream collect {
        case Success(e) ⇒ e
      }).toMap
      val cl = CastleLastMoveTime(
        castles = getAs[String](d1, "cs").fold(Castles.all)(Castles.apply),
        lastMove = getAs[String](d1, "lm") flatMap parseLastMove,
        lastMoveTime = None)
      val binCL = BinaryFormat.castleLastMoveTime write cl
      val bsonCL = BSONBinary(binCL.value, Subtype.UserDefinedSubtype)
      val d2 = d1 + ("cl" -> bsonCL)
      BSONDocument {
        (d2 filterKeys {
          case "cs" | "lm" | "lmt" ⇒ false
          case _                   ⇒ true
        }).toStream
      }
    }

    def convertPrll(docs: Docs): Docs = {
      Future.traverse(docs) { o ⇒
        Future { convert(o) }
      }
    }.await(5 seconds)

    def migrate: Funit = {

      val docsEnumerator: Enumerator[Docs] = oldGameColl
        .find(BSONDocument())
        .sort(BSONDocument("ca" -> -1))
        .batch(batchSize)
        .cursor[BSONDocument].enumerateBulks(limit)

      val docIteratee: Iteratee[Doc, Int] = tube.gameTube.coll.bulkInsertIteratee(
        bulkSize = batchSize,
        bulkByteSize = 1024 * 1024 * 8 /* 8MB */ )

      docsEnumerator |>>> Iteratee.foreach[Docs] { docs ⇒
        {
          Enumerator.enumerate(convertPrll(docs)) |>>> docIteratee
        }.await(10 seconds)
      }

    }

    (tube.gameTube.coll.drop recover {
      case e ⇒ println(e)
    }) >> migrate
  }
}
