package lila.puzzle

import akka.stream.scaladsl._
import chess.opening.{ FullOpening, FullOpeningDB, OpeningFamily, OpeningVariation }
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.common.{ LilaOpening, LilaStream }
import lila.db.dsl._
import lila.game.GameRepo
import lila.i18n.{ I18nKey, I18nKeys => trans }
import lila.memo.CacheApi

case class PuzzleOpeningCollection(all: List[PuzzleOpening.WithCount]) {

  import LilaOpening._
  import PuzzleOpening._

  val byKey: Map[Key, WithCount] = all.view.map { op =>
    op.opening.key -> op
  }.toMap
  val treeMap: TreeMap = all.foldLeft[TreeMap](Map.empty) { case (tree, op) =>
    tree.updatedWith(op.opening.family) {
      case None => (op.count, op.opening.ref, op.opening.variation.isDefined ?? Set(op)).some
      case Some((famCount, famRef, ops)) =>
        (famCount, famRef, if (op.opening.variation.isDefined) ops incl op else ops).some
    }
  }
  val treePopular: TreeList = treeMap.toList
    .map { case (family, (famCount, famRef, ops)) =>
      FamilyWithCount(PuzzleOpeningFamily(family, famRef), famCount) -> ops.toList.sortBy(-_.count)
    }
    .sortBy(-_._1.count)
  val treeAlphabetical: TreeList = treePopular
    .map { case (fam, ops) =>
      fam -> ops.sortBy(_.opening.name.value)
    }
    .sortBy(_._1.family.family.name)

  def treeList(order: Order) = order match {
    case Order.Popular      => treePopular
    case Order.Alphabetical => treeAlphabetical
  }
}

final class PuzzleOpeningApi(colls: PuzzleColls, gameRepo: GameRepo, cacheApi: CacheApi)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: akka.stream.Materializer
) {
  import BsonHandlers._
  import LilaOpening._
  import PuzzleOpening._

  private val collectionCache =
    cacheApi.unit[PuzzleOpeningCollection] {
      _.refreshAfterWrite(1 day)
        .buildAsyncFuture { _ =>
          import Puzzle.BSONFields._
          colls.puzzle {
            _.aggregateList(maxOpenings) { framework =>
              import framework._
              UnwindField(opening) -> List(
                PipelineOperator($doc("$sortByCount" -> s"$$$opening")),
                Limit(maxOpenings)
              )
            }.map {
              _.flatMap { obj =>
                for {
                  key     <- obj string "_id" map LilaOpening.Key
                  opening <- LilaOpening(key)
                  if opening.variation.fold(true)(_ != otherVariations)
                  count <- obj int "count"
                } yield WithCount(opening, count)
              }
            } map PuzzleOpeningCollection
          }
        }
    }

  def collection: Fu[PuzzleOpeningCollection] =
    collectionCache get {}

  def count(key: LilaOpening.Key): Fu[Int] =
    collection dmap { _.byKey.get(key).??(_.count) }

  def addAllMissing: Funit =
    colls.puzzle {
      _.find(
        $doc(
          Puzzle.BSONFields.opening $exists false,
          Puzzle.BSONFields.themes $nin List(PuzzleTheme.equality.key, PuzzleTheme.endgame.key),
          Puzzle.BSONFields.fen $endsWith """\s[|1]\d""" // up to move 19!
        )
      )
        .cursor[Puzzle]()
        .documentSource()
        .mapAsyncUnordered(4)(addMissing)
        .runWith(LilaStream.sinkCount)
        .chronometer
        .log(logger)(count => s"Done adding $count puzzle openings")
        .result
        .void
    }

  private def addMissing(puzzle: Puzzle): Funit = gameRepo gameFromSecondary puzzle.gameId flatMap {
    _ ?? { game =>
      FullOpeningDB.search(game.pgnMoves).map(_.opening) match {
        case None =>
          fuccess {
            logger warn s"No opening for https://lichess.org/training/${puzzle.id}"
          }
        case Some(o) =>
          val variation = o.variation | otherVariations
          val keys = List(
            LilaOpening.nameToKey(Name(o.family.name)),
            LilaOpening.nameToKey(Name(s"${o.family.name}: ${variation.name}"))
          )
          colls.puzzle {
            _.updateField($id(puzzle.id), Puzzle.BSONFields.opening, keys).void
          }
      }
    }
  }
}

case class PuzzleOpeningFamily(family: OpeningFamily, ref: FullOpening) {
  lazy val key = LilaOpening.nameToKey(LilaOpening.Name(family.name))
}

object PuzzleOpening {

  val maxOpenings = 1000

  type Count = Int
  type Name  = String

  val otherVariations = OpeningVariation("Other variations")

  case class WithCount(opening: LilaOpening, count: Count)
  case class FamilyWithCount(family: PuzzleOpeningFamily, count: Count)

  type TreeMap  = Map[OpeningFamily, (Count, FullOpening, Set[WithCount])]
  type TreeList = List[(FamilyWithCount, List[WithCount])]

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
