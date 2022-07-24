package lila.puzzle

import akka.stream.scaladsl._
import chess.opening.{ FullOpening, FullOpeningDB, OpeningFamily, OpeningVariation }
import reactivemongo.akkastream.cursorProducer
import scala.concurrent.duration._

import lila.common.{ LilaOpening, LilaOpeningFamily, LilaStream }
import lila.db.dsl._
import lila.game.GameRepo
import lila.i18n.{ I18nKey, I18nKeys => trans }
import lila.memo.{ CacheApi, MongoCache }

case class PuzzleOpeningCollection(
    families: List[PuzzleOpening.FamilyWithCount], // most popular first
    openings: List[PuzzleOpening.WithCount]        // most popular first
) {

  import LilaOpening._
  import PuzzleOpening._

  val familyMap  = families.view.map { fam => fam.family.key -> fam }.toMap
  val openingMap = openings.view.map { op => op.opening.key -> op }.toMap

  val treeMap: TreeMap = openings.foldLeft[TreeMap](Map.empty) { case (tree, op) =>
    tree.updatedWith(op.opening.family) {
      case None =>
        (
          families.find(_.family.key == op.opening.family.key).??(_.count),
          op.opening.ref.variation.isDefined ?? Set(op)
        ).some
      case Some((famCount, ops)) =>
        (famCount, if (op.opening.ref.variation.isDefined) ops incl op else ops).some
    }
  }
  val treePopular: TreeList = treeMap.toList
    .map { case (family, (famCount, ops)) =>
      FamilyWithCount(family, famCount) -> ops.toList.sortBy(-_.count)
    }
    .sortBy(-_._1.count)
  val treeAlphabetical: TreeList = treePopular
    .map { case (fam, ops) =>
      fam -> ops.sortBy(_.opening.name.value)
    }
    .sortBy(_._1.family.name.value)

  def treeList(order: Order) = order match {
    case Order.Popular      => treePopular
    case Order.Alphabetical => treeAlphabetical
  }

  def makeMine(myFams: List[LilaOpeningFamily], myVars: List[LilaOpening]) = Mine(
    families = myFams.filter(fam => familyMap.contains(fam.key)),
    variations = myVars.filter(op => openingMap.contains(op.key))
  )
}

final class PuzzleOpeningApi(
    colls: PuzzleColls,
    gameRepo: GameRepo,
    cacheApi: CacheApi,
    mongoCache: MongoCache.Api
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: akka.stream.Materializer
) {
  import BsonHandlers._
  import LilaOpening._
  import PuzzleOpening._

  private val countedCache = mongoCache.unitNoHeap[List[Bdoc]]("puzzle:opening:counted", 24 hours) { _ =>
    import Puzzle.BSONFields._
    colls.puzzle {
      _.aggregateList(maxOpenings) { framework =>
        import framework._
        UnwindField(opening) -> List(
          PipelineOperator($doc("$sortByCount" -> s"$$$opening")),
          Limit(maxOpenings)
        )
      }
    }
  }

  private val collectionCache =
    cacheApi.unit[PuzzleOpeningCollection] {
      _.refreshAfterWrite(1 hour)
        .buildAsyncFuture { _ =>
          countedCache.get(()) map {
            _.foldLeft(PuzzleOpeningCollection(Nil, Nil)) { case (acc, obj) =>
              val count = ~obj.int("count")
              obj.string("_id").fold(acc) { keyStr =>
                LilaOpeningFamily.find(keyStr) match {
                  case Some(fam) => acc.copy(families = FamilyWithCount(fam, count) :: acc.families)
                  case None =>
                    LilaOpening
                      .find(keyStr)
                      .filter(_.ref.variation != LilaOpening.otherVariations)
                      .fold(acc) { op =>
                        acc.copy(openings = PuzzleOpening.WithCount(op, count) :: acc.openings)
                      }
                }
              }
            }
          } map { case PuzzleOpeningCollection(families, openings) =>
            PuzzleOpeningCollection(families.reverse, openings.reverse)
          }
        }
    }

  def getClosestTo(
      opening: FullOpening
  ): Fu[Option[Either[PuzzleOpening.FamilyWithCount, PuzzleOpening.WithCount]]] =
    LilaOpening(opening) ?? { lilaOp =>
      collection map { coll =>
        coll.openingMap.get(lilaOp.key).map(Right.apply) orElse {
          coll.familyMap.get(lilaOp.family.key).map(Left.apply)
        }
      }
    }

  def find(family: LilaOpeningFamily): Fu[Option[PuzzleOpening.FamilyWithCount]] =
    collection map { _.familyMap.get(family.key) }

  def collection: Fu[PuzzleOpeningCollection] =
    collectionCache get {}

  def count(key: Either[LilaOpeningFamily.Key, LilaOpening.Key]): Fu[Int] =
    collection dmap { coll =>
      key.fold(f => coll.familyMap.get(f).??(_.count), o => coll.openingMap.get(o).??(_.count))
    }

  private[puzzle] def addAllMissing: Funit = colls.puzzle {
    _.find(
      $doc(
        Puzzle.BSONFields.opening $exists false,
        Puzzle.BSONFields.themes $nin List(PuzzleTheme.equality.key, PuzzleTheme.endgame.key),
        Puzzle.BSONFields.fen $endsWith """\s[|1]\d""" // up to move 19!
      )
    )
      .cursor[Puzzle]()
      .documentSource()
      .mapAsyncUnordered(4)(updateOpening)
      .runWith(LilaStream.sinkCount)
      .chronometer
      .log(logger)(count => s"Done adding $count puzzle openings")
      .result
      .void
  }

  def recomputeAll: Funit = colls.puzzle {
    _.find($doc(Puzzle.BSONFields.opening $exists true))
      .cursor[Puzzle]()
      .documentSource()
      .mapAsyncUnordered(2)(updateOpening)
      .runWith(LilaStream.sinkCount)
      .chronometer
      .log(logger)(count => s"Done updating $count puzzle openings")
      .result
      .void
  }

  private def updateOpening(puzzle: Puzzle): Funit = gameRepo gameFromSecondary puzzle.gameId flatMap {
    _ ?? { game =>
      FullOpeningDB.search(game.pgnMoves).map(_.opening).flatMap(LilaOpening.apply) match {
        case None =>
          fuccess {
            logger warn s"No opening for https://lichess.org/training/${puzzle.id}"
          }
        case Some(o) =>
          val keys = List(o.family.key, o.key).map(_.value)
          colls.puzzle {
            _.updateField($id(puzzle.id), Puzzle.BSONFields.opening, keys).void
          }
      }
    }
  }
}

object PuzzleOpening {

  val maxOpenings = 1000

  type Count = Int
  type Name  = String

  case class WithCount(opening: LilaOpening, count: Count)
  case class FamilyWithCount(family: LilaOpeningFamily, count: Count)

  type TreeMap  = Map[LilaOpeningFamily, (Count, Set[WithCount])]
  type TreeList = List[(FamilyWithCount, List[WithCount])]

  case class Mine(families: List[LilaOpeningFamily], variations: List[LilaOpening]) {
    lazy val familyKeys    = families.view.map(_.key).toSet
    lazy val variationKeys = variations.view.map(_.key).toSet
  }

  sealed abstract class Order(val key: String, val name: I18nKey)

  object Order {
    case object Popular      extends Order("popular", trans.study.mostPopular)
    case object Alphabetical extends Order("alphabetical", trans.study.alphabetical)

    val default = Popular
    val all     = List(Popular, Alphabetical)
    private val byKey: Map[String, Order] = all.map { o =>
      o.key -> o
    }.toMap
    def apply(key: String): Order = byKey.getOrElse(key, default)
  }
}
