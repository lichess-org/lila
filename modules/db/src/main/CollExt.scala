package lila.db

import scala.collection.breakOut
import scala.collection.generic.CanBuildFrom

import scala.concurrent.ExecutionContext

import scala.util.{ Success, Failure }

import com.github.ghik.silencer.silent

import reactivemongo.bson.{
  BSONDocument,
  BSONDocumentHandler,
  BSONDocumentWriter,
  BSONDocumentReader
}

import reactivemongo.api.{
  BSONSerializationPack,
  Cursor,
  CollectionProducer,
  DB,
  FailoverStrategy,
  ReadConcern,
  ReadPreference
}
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.collections.bson.BSONCollection

final class CollectionExt private[db] (
    val underlying: BSONCollection
) extends GenericCollection[BSONSerializationPack.type] {

  val pack = BSONSerializationPack

  protected val BatchCommands =
    reactivemongo.api.collections.bson.BSONBatchCommands

  implicit private def docIdentityWriter: BSONDocumentWriter[BSONDocument] =
    pack.IdentityWriter

  implicit private def docIdentityReader: BSONDocumentReader[BSONDocument] =
    pack.IdentityReader

  // Extensions
  import dsl._

  // TODO: Remove once default args are back on GenericCollection.find
  override def find[Q: BSONDocumentWriter](query: Q) =
    super.find(query, Option.empty[Bdoc])

  // TODO: Remove once default args are back on GenericCollection.distinct
  def distinct[T, M[_] <: Iterable[_]](
    key: String,
    query: BSONDocument
  )(implicit
    reader: BSONSerializationPack.NarrowValueReader[T],
    ec: ExecutionContext, cbf: CanBuildFrom[M[_], T, M[T]]
  ): Fu[M[T]] = super.distinct[T, M](key, Some(query),
    ReadConcern.Local, Option.empty)(reader, ec, cbf)

  def byId[D: BSONDocumentReader, I: BSONValueWriter](id: I): Fu[Option[D]] =
    underlying.find($id(id), Option.empty[Bdoc]).one[D]

  def byId[D: BSONDocumentReader](id: String): Fu[Option[D]] =
    underlying.find($id(id), Option.empty[Bdoc]).one[D]

  def byId[D: BSONDocumentReader](id: String, projection: Bdoc): Fu[Option[D]] = underlying.find($id(id), Option(projection)).one[D]

  def byId[D: BSONDocumentReader](id: Int): Fu[Option[D]] =
    underlying.find($id(id), Option.empty[Bdoc]).one[D]

  def byIds[D: BSONDocumentReader, I: BSONValueWriter](
    ids: Iterable[I], readPreference: ReadPreference
  ): Fu[List[D]] =
    underlying.find($inIds(ids), Option.empty[Bdoc]).cursor[D](readPreference)
      .collect[List](-1, Cursor.ContOnError[List[D]]())

  def byIds[D: BSONDocumentReader](ids: Iterable[String]): Fu[List[D]] =
    underlying.find($inIds(ids), Option.empty[Bdoc])
      .cursor[D](ReadPreference.primary)
      .collect[List](-1, Cursor.ContOnError[List[D]]())

  def byIds[D: BSONDocumentReader](
    ids: Iterable[String], readPreference: ReadPreference
  ): Fu[List[D]] =
    underlying.find($inIds(ids), Option.empty[Bdoc]).cursor[D](readPreference)
      .collect[List](-1, Cursor.ContOnError[List[D]]())

  def countSel(
    selector: Bdoc,
    readPreference: ReadPreference = ReadPreference.primary
  ): Fu[Int] = underlying.withReadPreference(readPreference).count(
    Some(selector), skip = 0, limit = None,
    hint = None, readConcern = ReadConcern.Local
  ).map(_.toInt)

  def exists(selector: Bdoc, readPreference: ReadPreference = ReadPreference.primary): Fu[Boolean] = countSel(selector, readPreference).dmap(0!=)

  def byOrderedIds[D: BSONDocumentReader, I: BSONValueWriter](ids: Iterable[I], projection: Option[Bdoc] = None, readPreference: ReadPreference = ReadPreference.primary)(docId: D => I): Fu[List[D]] =
    underlying.find($inIds(ids), projection)
      .cursor[D](readPreference = readPreference)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[D]]())
      .map { docs =>
        val docsMap: Map[I, D] = docs.map(u => docId(u) -> u)(breakOut)
        ids.flatMap(docsMap.get)(breakOut)
      }

  def optionsByOrderedIds[D: BSONDocumentReader, I: BSONValueWriter](
    ids: Iterable[I],
    readPreference: ReadPreference = ReadPreference.primary
  )(docId: D => I): Fu[List[Option[D]]] =
    byIds[D, I](ids, readPreference) map { docs =>
      val docsMap: Map[I, D] = docs.map(u => docId(u) -> u)(breakOut)
      ids.map(docsMap.get)(breakOut)
    }

  def idsMap[D: BSONDocumentReader, I: BSONValueWriter](ids: Iterable[I], readPreference: ReadPreference = ReadPreference.primary)(docId: D => I): Fu[Map[I, D]] =
    byIds[D, I](ids, readPreference) map { docs =>
      docs.map(u => docId(u) -> u)(breakOut)
    }

  def primitive[V: BSONValueReader](selector: Bdoc, field: String): Fu[List[V]] =
    underlying.find(selector, Some($doc(field -> true)))
      .cursor[Bdoc]().collect[List](-1, Cursor.ContOnError[List[Bdoc]]()).dmap {
        _ flatMap { _.getAs[V](field) }
      }

  def primitive[V: BSONValueReader](selector: Bdoc, sort: Bdoc, field: String): Fu[List[V]] =
    underlying.find(selector, Some($doc(field -> true))).sort(sort)
      .cursor[Bdoc]().collect[List](-1, Cursor.ContOnError[List[Bdoc]]()).dmap {
        _ flatMap { _.getAs[V](field) }
      }

  def primitive[V: BSONValueReader](selector: Bdoc, sort: Bdoc, nb: Int, field: String): Fu[List[V]] =
    underlying.find(selector, Some($doc(field -> true))).sort(sort)
      .cursor[Bdoc]().collect[List](-1, Cursor.ContOnError[List[Bdoc]]()).dmap {
        _ flatMap { _.getAs[V](field) }
      }

  def primitiveOne[V: BSONValueReader](selector: Bdoc, field: String): Fu[Option[V]] =
    underlying.find(selector, Some($doc(field -> true))).one[Bdoc].dmap {
      _ flatMap { _.getAs[V](field) }
    }

  def primitiveOne[V: BSONValueReader](
    selector: Bdoc, sort: Bdoc, field: String
  ): Fu[Option[V]] =
    underlying.find(selector, Some($doc(field -> true)))
      .sort(sort)
      .one[Bdoc]
      .dmap {
        _ flatMap { _.getAs[V](field) }
      }

  def updateField[V: BSONValueWriter](selector: Bdoc, field: String, value: V) =
    underlying.update.one(selector, $set(field -> value))

  def incField(selector: Bdoc, field: String, value: Int = 1) =
    underlying.update.one(selector, $inc(field -> value))

  def unsetField(selector: Bdoc, field: String, multi: Boolean = false) =
    underlying.update.one(selector, $unset(field), multi = multi)

  def fetchUpdate[D: BSONDocumentHandler](selector: Bdoc)(update: D => Bdoc): Funit = underlying.find(selector, Option.empty[Bdoc]).one[D].flatMap {
    _ ?? { doc =>
      underlying.update.one(selector, update(doc)).void
    }
  }

  def aggregateList(
    firstOperator: underlying.PipelineOperator,
    otherOperators: List[underlying.PipelineOperator] = Nil,
    maxDocs: Int,
    readPreference: ReadPreference = ReadPreference.primary,
    allowDiskUse: Boolean = false
  ): Fu[List[Bdoc]] = underlying.aggregatorContext[Bdoc](
    firstOperator,
    otherOperators,
    readPreference = readPreference
  ).prepared.cursor.collect[List](maxDocs, Cursor.FailOnError[List[Bdoc]]())

  def aggregateOne(
    firstOperator: underlying.PipelineOperator,
    otherOperators: List[underlying.PipelineOperator] = Nil,
    readPreference: ReadPreference = ReadPreference.primary
  ): Fu[Option[Bdoc]] =
    underlying.aggregatorContext[Bdoc](firstOperator, otherOperators,
      readPreference = readPreference).prepared.cursor.headOption

  def distinctWithReadPreference[T, M[_] <: Iterable[_]](
    key: String,
    selector: Option[Bdoc],
    readPreference: ReadPreference
  )(implicit reader: BSONSerializationPack.NarrowValueReader[T], cbf: CanBuildFrom[M[_], T, M[T]]): Fu[M[T]] = {
    underlying.withReadPreference(readPreference).distinct[T, M](
      key, selector, ReadConcern.Local, collation = None
    )
  }

  // ---

  @inline def db: DB = underlying.db

  @inline def name: String = underlying.name

  def failoverStrategy: FailoverStrategy = underlying.failoverStrategy

  def withReadPreference(readPreference: ReadPreference): CollectionExt =
    new CollectionExt(underlying.withReadPreference(readPreference))
}

trait CollExt { self: dsl =>
  implicit object CollProducer extends CollectionProducer[CollectionExt] {

    private val underlying = implicitly[CollectionProducer[BSONCollection]]

    def apply(db: DB, name: String, failoverStrategy: FailoverStrategy = FailoverStrategy()): CollectionExt = new CollectionExt(underlying(db, name, failoverStrategy))
  }
}
