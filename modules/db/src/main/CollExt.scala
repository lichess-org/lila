package lila.db

import scala.collection.breakOut
import scala.collection.generic.CanBuildFrom
import scala.util.{ Success, Failure }

import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONBatchCommands._
import reactivemongo.api.commands.GetLastError
import reactivemongo.bson._
import reactivemongo.core.protocol.MongoWireVersion

trait CollExt { self: dsl with QueryBuilderExt =>

  final implicit class ExtendColl(coll: Coll) {

    def uno[D: BSONDocumentReader](selector: Bdoc): Fu[Option[D]] =
      coll.find(selector).uno[D]

    def uno[D: BSONDocumentReader](selector: Bdoc, projection: Bdoc): Fu[Option[D]] =
      coll.find(selector, projection).uno[D]

    def list[D: BSONDocumentReader](selector: Bdoc, readPreference: ReadPreference = ReadPreference.primary): Fu[List[D]] =
      coll.find(selector).list[D](Int.MaxValue, readPreference = readPreference)

    def list[D: BSONDocumentReader](selector: Bdoc, limit: Int): Fu[List[D]] =
      coll.find(selector).list[D](limit = limit)

    def byId[D: BSONDocumentReader, I: BSONValueWriter](id: I): Fu[Option[D]] =
      uno[D]($id(id))

    def byId[D: BSONDocumentReader](id: String): Fu[Option[D]] = uno[D]($id(id))
    def byId[D: BSONDocumentReader](id: String, projection: Bdoc): Fu[Option[D]] = uno[D]($id(id), projection)

    def byId[D: BSONDocumentReader](id: Int): Fu[Option[D]] = uno[D]($id(id))

    def byIds[D: BSONDocumentReader, I: BSONValueWriter](ids: Iterable[I], readPreference: ReadPreference): Fu[List[D]] =
      list[D]($inIds(ids))

    def byIds[D: BSONDocumentReader](ids: Iterable[String], readPreference: ReadPreference = ReadPreference.primary): Fu[List[D]] =
      byIds[D, String](ids, readPreference)

    def countSel(
      selector: Bdoc,
      readPreference: ReadPreference = ReadPreference.primary
    ): Fu[Int] =
      coll.runValueCommand(
        CountCommand.Count(query = selector.some, limit = 0, skip = 0, hint = None),
        readPreference
      )

    def exists(selector: Bdoc, readPreference: ReadPreference = ReadPreference.primary): Fu[Boolean] =
      countSel(selector, readPreference).dmap(0!=)

    def byOrderedIds[D: BSONDocumentReader, I: BSONValueWriter](ids: Iterable[I], projection: Option[Bdoc] = None, readPreference: ReadPreference = ReadPreference.primary)(docId: D => I): Fu[List[D]] =
      projection.fold(coll.find($inIds(ids))) { proj =>
        coll.find($inIds(ids), proj)
      }.cursor[D](readPreference = readPreference)
        .collect[List](Int.MaxValue, err = Cursor.FailOnError[List[D]]())
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
      coll.find(selector, $doc(field -> true))
        .list[Bdoc]()
        .dmap {
          _ flatMap { _.getAs[V](field) }
        }

    def primitive[V: BSONValueReader](selector: Bdoc, sort: Bdoc, field: String): Fu[List[V]] =
      coll.find(selector, $doc(field -> true))
        .sort(sort)
        .list[Bdoc]()
        .dmap {
          _ flatMap { _.getAs[V](field) }
        }

    def primitive[V: BSONValueReader](selector: Bdoc, sort: Bdoc, nb: Int, field: String): Fu[List[V]] =
      coll.find(selector, $doc(field -> true))
        .sort(sort)
        .list[Bdoc](nb)
        .dmap {
          _ flatMap { _.getAs[V](field) }
        }

    def primitiveOne[V: BSONValueReader](selector: Bdoc, field: String): Fu[Option[V]] =
      coll.find(selector, $doc(field -> true))
        .uno[Bdoc]
        .dmap {
          _ flatMap { _.getAs[V](field) }
        }

    def primitiveOne[V: BSONValueReader](selector: Bdoc, sort: Bdoc, field: String): Fu[Option[V]] =
      coll.find(selector, $doc(field -> true))
        .sort(sort)
        .uno[Bdoc]
        .dmap {
          _ flatMap { _.getAs[V](field) }
        }

    def updateField[V: BSONValueWriter](selector: Bdoc, field: String, value: V) =
      coll.update(selector, $set(field -> value))

    def updateFieldUnchecked[V: BSONValueWriter](selector: Bdoc, field: String, value: V): Unit =
      coll.update(selector, $set(field -> value), writeConcern = GetLastError.Unacknowledged)

    def incField(selector: Bdoc, field: String, value: Int = 1) =
      coll.update(selector, $inc(field -> value))

    def incFieldUnchecked(selector: Bdoc, field: String, value: Int = 1): Unit =
      coll.update(selector, $inc(field -> value), writeConcern = GetLastError.Unacknowledged)

    def unsetField(selector: Bdoc, field: String, multi: Boolean = false) =
      coll.update(selector, $unset(field), multi = multi)

    def fetchUpdate[D: BSONDocumentHandler](selector: Bdoc)(update: D => Bdoc): Funit =
      uno[D](selector) flatMap {
        _ ?? { doc =>
          coll.update(selector, update(doc)).void
        }
      }

    // sadly we can't access the connection metadata
    private val mongoWireVersion = MongoWireVersion.V34

    def aggregateList(
      firstOperator: AggregationFramework.PipelineOperator,
      otherOperators: List[AggregationFramework.PipelineOperator] = Nil,
      maxDocs: Int,
      readPreference: ReadPreference = ReadPreference.primary,
      allowDiskUse: Boolean = false
    ): Fu[List[Bdoc]] = coll.aggregatorContext[Bdoc](
      firstOperator,
      otherOperators,
      readPreference = readPreference
    ).prepared[Cursor](CursorProducer.defaultCursorProducer[Bdoc]).cursor.collect[List](maxDocs = maxDocs, Cursor.FailOnError[List[Bdoc]]())

    def aggregateOne(
      firstOperator: AggregationFramework.PipelineOperator,
      otherOperators: List[AggregationFramework.PipelineOperator] = Nil,
      readPreference: ReadPreference = ReadPreference.primary
    ): Fu[Option[Bdoc]] =
      coll.aggregatorContext[Bdoc](firstOperator, otherOperators, readPreference = readPreference)
        .prepared[Cursor](CursorProducer.defaultCursorProducer[Bdoc]).cursor.headOption

    def distinctWithReadPreference[T, M[_] <: Iterable[_]](
      key: String,
      selector: Option[Bdoc],
      readPreference: ReadPreference
    )(implicit reader: BSONValueReader[T], cbf: CanBuildFrom[M[_], T, M[T]]): Fu[M[T]] = {
      implicit val widenReader = pack.widenReader(reader)
      coll.runCommand(DistinctCommand.Distinct(
        key, selector, ReadConcern.Local, mongoWireVersion
      ), readPreference).flatMap {
        _.result[T, M] match {
          case Failure(cause) => scala.concurrent.Future.failed[M[T]](cause)
          case Success(result) => fuccess(result)
        }
      }
    }
  }
}
