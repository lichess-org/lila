package lila.game

import scala.concurrent.duration._
import scala.concurrent.Future

import play.api.libs.iteratee._
import reactivemongo.bson._

import lila.db.Implicits._

object PgnBinaryMigration {

  type Doc = BSONDocument
  type Docs = Iterator[Doc]

  def apply(db: lila.db.Env, system: akka.actor.ActorSystem) = {

    val oldPgnColl = db("pgn")
    val repo = PgnRepo
    val batchSize = 100
    val limit = 20 * 1000 * 1000

    def convert(o: Doc): Option[Doc] = for {
      id ← o.getAs[String]("_id")
      moves ← o.getAs[String]("p")
      if moves.trim.nonEmpty
      done ← try {
        Some(PgnRepo.make(id, moves.split(' ').toList))
      }
      catch {
        case e: scala.MatchError ⇒ {
          println(s"Invalid: $e in $id")
          None
        }
        case e: Exception ⇒ {
          println(s"ERROR: $e in $id")
          None
        }
      }
    } yield done

    def convertPrll(docs: Docs): Docs = {
      Future.traverse(docs) { o ⇒
        Future { convert(o) }
      } map (_.flatten)
    }.await(5 seconds)

    def migrate: Funit = {

      val docsEnumerator: Enumerator[Docs] = oldPgnColl
        .find(BSONDocument())
        .batch(batchSize)
        .cursor[BSONDocument].enumerateBulks(limit)

      val docIteratee: Iteratee[Doc, Int] = tube.pgnColl.bulkInsertIteratee(
        bulkSize = batchSize,
        bulkByteSize = 1024 * 1024 * 8 /* 8MB */ )

      docsEnumerator |>>> Iteratee.foreach[Docs] { docs ⇒
        {
          Enumerator.enumerate(convertPrll(docs)) |>>> docIteratee
        }.await(10 seconds)
      }

    } 

    (tube.pgnColl.drop recover {
      case e ⇒ println(e)
    }) >> migrate
  }
}
