package lila.puzzle

import akka.stream.scaladsl._
import chess.opening.{ FullOpening, FullOpeningDB, OpeningFamily, OpeningVariation }
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.common.LilaStream
import lila.db.dsl._
import lila.game.GameRepo
import lila.memo.CacheApi

case class PuzzleOpening(ref: FullOpening) {
  val name: PuzzleOpening.Name = ref.variation.fold(ref.family.name)(v => s"${ref.family.name}: ${v.name}")
  val key: PuzzleOpening.Key   = PuzzleOpening.nameToKey(name)
  def family                   = ref.family
  def variation                = ref.variation
}

case class PuzzleOpeningCollection(all: List[PuzzleOpening.WithCount]) {
  import PuzzleOpening._
  val byKey: Map[PuzzleOpening.Key, PuzzleOpening.WithCount] = all.view.map { op =>
    op.opening.key -> op
  }.toMap
  val treeMap: TreeMap = all.foldLeft[TreeMap](Map.empty) { case (tree, op) =>
    tree.updatedWith(op.opening.family) { prev =>
      (~prev).incl(op).some
    }
  }
  val treeList: TreeList = treeMap.toList
    .map { case (family, ops) =>
      val count = ops.headOption.filter(_.opening.variation.isEmpty).fold(ops.map(_.count).sum)(_.count)
      (family, count) -> ops.toList.sortBy(-_.count)
    }
    .sortBy(-_._1._2)
}

final class PuzzleOpeningApi(colls: PuzzleColls, gameRepo: GameRepo, cacheApi: CacheApi)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: akka.stream.Materializer
) {
  import BsonHandlers._
  import PuzzleOpening._

  private val collectionCache =
    cacheApi.unit[PuzzleOpeningCollection] {
      _.refreshAfterWrite(1 day)
        .buildAsyncFuture { _ =>
          import Puzzle.BSONFields._
          colls.puzzle {
            _.aggregateList(PuzzleOpening.maxOpenings) { framework =>
              import framework._
              UnwindField(opening) -> List(
                PipelineOperator($doc("$sortByCount" -> s"$$$opening")),
                Limit(PuzzleOpening.maxOpenings)
              )
            }.map {
              _.flatMap { obj =>
                for {
                  key     <- obj string "_id" map PuzzleOpening.Key
                  opening <- PuzzleOpening(key)
                  count   <- obj int "count"
                } yield PuzzleOpening.WithCount(opening, count)
              }
            } map PuzzleOpeningCollection
          }
        }
    }

  def collection: Fu[PuzzleOpeningCollection] =
    collectionCache get {}

  def count(key: PuzzleOpening.Key): Fu[Int] =
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
        .mapAsyncUnordered(12)(addMissing)
        .runWith(LilaStream.sinkCount)
        .chronometer
        .log(logger)(count => s"Done adding $count puzzle openings")
        .result
        .void
    }

  private def addMissing(puzzle: Puzzle): Funit = gameRepo game puzzle.gameId flatMap {
    _ ?? { game =>
      FullOpeningDB.search(game.pgnMoves).map(_.opening) match {
        case None =>
          fuccess {
            logger warn s"No opening for https://lichess.org/training/${puzzle.id}"
          }
        case Some(o) =>
          val keys =
            nameToKey(o.family.name) :: o.variation.map(v => nameToKey(s"${o.family.name}: ${v.name}")).toList
          val openings = keys flatMap PuzzleOpening.apply
          colls.puzzle {
            _.updateField(
              $id(puzzle.id),
              Puzzle.BSONFields.opening,
              openings.map(_.key)
            ).void
          }
      }
    }
  }
}

object PuzzleOpening {

  case class Key(value: String) extends AnyVal with StringValue

  val maxOpenings = 500

  import java.text.Normalizer

  type Count = Int
  type Name  = String

  case class WithCount(opening: PuzzleOpening, count: Count)

  type TreeMap = Map[OpeningFamily, Set[PuzzleOpening.WithCount]]
  // sorted by counts
  type TreeList = List[((OpeningFamily, Count), List[PuzzleOpening.WithCount])]

  implicit val keyIso = lila.common.Iso.string[Key](Key.apply, _.value)

  def nameToKey(name: Name) = Key {
    Normalizer
      .normalize(name, Normalizer.Form.NFD)  // split an accented letter in the base letter and the accent
      .replaceAllIn("[\u0300-\u036f]".r, "") // remove all previously split accents
      .replaceAllIn("""\s+""".r, "_")
      .replaceAllIn("""[^\w\-]+""".r, "")
  }

  def apply(key: Key): Option[PuzzleOpening] = openings get key

  def find(key: String): Option[PuzzleOpening] = apply(Key(key))

  lazy val openings: Map[Key, PuzzleOpening] = FullOpeningDB.all.foldLeft(Map.empty[Key, PuzzleOpening]) {
    case (acc, fullOp) =>
      val op = PuzzleOpening(fullOp)
      if (acc.contains(op.key)) acc else acc.updated(op.key, op)
  }
}
