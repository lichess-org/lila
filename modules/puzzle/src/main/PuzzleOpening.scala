package lila.puzzle

import chess.opening.{ Opening, OpeningDb, OpeningFamily }
import reactivemongo.akkastream.cursorProducer

import lila.common.{ LilaOpeningFamily, LilaStream, SimpleOpening }
import lila.core.i18n.I18nKey
import lila.db.dsl.{ *, given }
import lila.memo.{ CacheApi, MongoCache }
import lila.memo.CacheApi.buildAsyncTimeout

case class PuzzleOpeningCollection(
    families: List[PuzzleOpening.FamilyWithCount], // most popular first
    openings: List[PuzzleOpening.WithCount] // most popular first
):

  import SimpleOpening.*
  import PuzzleOpening.*

  val familyMap = families.mapBy(_.family.key)
  val openingMap = openings.mapBy(_.opening.key)

  val treeMap: TreeMap = openings.foldLeft[TreeMap](Map.empty): (tree, op) =>
    tree.updatedWith(op.opening.family):
      case None =>
        (
          families.find(_.family.key == op.opening.family.key).so(_.count),
          op.opening.ref.variation.isDefined.so(Set(op))
        ).some
      case Some(famCount, ops) =>
        (famCount, if op.opening.ref.variation.isDefined then ops.incl(op) else ops).some

  val treePopular: TreeList = treeMap.toList
    .map { case (family, (famCount, ops)) =>
      FamilyWithCount(family, famCount) -> ops.toList.sortBy(-_.count)
    }
    .sortBy(-_._1.count)

  val treeAlphabetical: TreeList = treePopular
    .map: (fam, ops) =>
      fam -> ops.sortBy(_.opening.name.value)
    .sortBy(_._1.family.name.value)

  def treeList(order: Order): TreeList = order match
    case Order.Popular => treePopular
    case Order.Alphabetical => treeAlphabetical

  def makeMine(myFams: List[LilaOpeningFamily], myVars: List[SimpleOpening]) = Mine(
    families = myFams.filter(fam => familyMap.contains(fam.key)),
    variations = myVars.filter(op => openingMap.contains(op.key))
  )

final class PuzzleOpeningApi(
    colls: PuzzleColls,
    gameRepo: lila.core.game.GameRepo,
    cacheApi: CacheApi,
    mongoCache: MongoCache.Api
)(using Executor, akka.stream.Materializer, Scheduler):
  import BsonHandlers.given
  import SimpleOpening.*
  import PuzzleOpening.*

  private val countedCache = mongoCache.unitNoHeap[List[Bdoc]]("puzzle:opening:counted", 24.hours): _ =>
    import Puzzle.BSONFields.*
    colls.puzzle:
      _.aggregateList(maxOpenings): framework =>
        import framework.*
        UnwindField(opening) -> List(
          PipelineOperator($doc("$sortByCount" -> s"$$$opening")),
          Limit(maxOpenings)
        )

  private val collectionCache =
    cacheApi.unit[PuzzleOpeningCollection]:
      _.refreshAfterWrite(1.hour).buildAsyncTimeout(1.minute): _ =>
        countedCache
          .get(())
          .map:
            _.foldLeft(PuzzleOpeningCollection(Nil, Nil)): (acc, obj) =>
              val count = ~obj.int("count")
              obj
                .string("_id")
                .fold(acc): keyStr =>
                  LilaOpeningFamily.find(keyStr) match
                    case Some(fam) => acc.copy(families = FamilyWithCount(fam, count) :: acc.families)
                    case None =>
                      SimpleOpening
                        .find(keyStr)
                        .filter(_.ref.variation != SimpleOpening.otherVariations)
                        .fold(acc): op =>
                          acc.copy(openings = PuzzleOpening.WithCount(op, count) :: acc.openings)
          .map { case PuzzleOpeningCollection(families, openings) =>
            PuzzleOpeningCollection(families.reverse, openings.reverse)
          }

  def getClosestTo(
      opening: Opening,
      quickOrNone: Boolean = false
  ): Fu[Option[Either[PuzzleOpening.FamilyWithCount, PuzzleOpening.WithCount]]] =
    SimpleOpening(opening).so: lilaOp =>
      (if quickOrNone then collectionQuickly else collection.map(some)).mapz: coll =>
        coll.openingMap
          .get(lilaOp.key)
          .map(Right.apply)
          .orElse(coll.familyMap.get(lilaOp.family.key).map(Left.apply))

  def find(family: OpeningFamily): Fu[Option[PuzzleOpening.FamilyWithCount]] =
    find(family.key.into(LilaOpeningFamily.Key))

  def find(family: LilaOpeningFamily.Key): Fu[Option[PuzzleOpening.FamilyWithCount]] =
    collection.map { _.familyMap.get(family) }

  def collection: Fu[PuzzleOpeningCollection] =
    collectionCache.get {}

  def collectionQuickly: Fu[Option[PuzzleOpeningCollection]] =
    collection.map(some).withTimeoutDefault(20.millis, none)

  def count(key: Either[LilaOpeningFamily.Key, SimpleOpening.Key]): Fu[Int] =
    collection.dmap: coll =>
      key.fold(f => coll.familyMap.get(f).so(_.count), o => coll.openingMap.get(o).so(_.count))

  def recomputeAll: Funit = colls.puzzle:
    _.find($doc(Puzzle.BSONFields.opening.$exists(true)))
      .cursor[Puzzle]()
      .documentSource()
      .mapAsyncUnordered(2)(updateOpening)
      .runWith(LilaStream.sinkCount)
      .chronometer
      .log(logger)(count => s"Done updating $count puzzle openings")
      .result
      .void

  private[puzzle] def updateOpening(puzzle: Puzzle): Funit =
    (!puzzle.hasTheme(PuzzleTheme.equality) && puzzle.initialPly < 36).so:
      gameRepo.gameFromSecondary(puzzle.gameId).flatMapz { game =>
        OpeningDb.search(game.sans).map(_.opening).flatMap(SimpleOpening.apply) match
          case None =>
            fuccess:
              logger.warn(s"No opening for https://lichess.org/training/${puzzle.id}")
          case Some(o) =>
            val keys = List(o.family.key.value, o.key.value)
            colls.puzzle:
              _.updateField($id(puzzle.id), Puzzle.BSONFields.opening, keys).void
      }

object PuzzleOpening:

  val maxOpenings = 1000

  type Count = Int
  type Name = String

  case class WithCount(opening: SimpleOpening, count: Count)
  case class FamilyWithCount(family: LilaOpeningFamily, count: Count)

  type TreeMap = Map[LilaOpeningFamily, (Count, Set[WithCount])]
  type TreeList = List[(FamilyWithCount, List[WithCount])]

  case class Mine(families: List[LilaOpeningFamily], variations: List[SimpleOpening]):
    lazy val familyKeys = families.view.map(_.key).toSet
    lazy val variationKeys = variations.view.map(_.key).toSet

  enum Order(val key: String, val name: I18nKey):
    case Popular extends Order("popular", I18nKey.study.mostPopular)
    case Alphabetical extends Order("alphabetical", I18nKey.study.alphabetical)

  object Order:
    val default = Popular
    val list = values.toList
    private val byKey = values.mapBy(_.key)
    def apply(key: String): Order = byKey.getOrElse(key, default)
