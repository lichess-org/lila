package lila.db

import scala.collection.Factory
import scala.annotation.nowarn

import reactivemongo.api._
import reactivemongo.api.bson._
import reactivemongo.api.{ WriteConcern => CWC }

trait CollExt { self: dsl with QueryBuilderExt =>

  implicit final class ExtendColl(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

    def secondaryPreferred = coll withReadPreference ReadPreference.secondaryPreferred
    def secondary          = coll withReadPreference ReadPreference.secondary

    def ext = this

    def one[D: BSONDocumentReader](selector: Bdoc): Fu[Option[D]] =
      coll.find(selector, none[Bdoc]).one[D]

    def one[D: BSONDocumentReader](selector: Bdoc, projection: Bdoc): Fu[Option[D]] =
      coll.find(selector, projection.some).one[D]

    def list[D: BSONDocumentReader](
        selector: Bdoc,
        readPreference: ReadPreference = ReadPreference.primary
    ): Fu[List[D]] =
      coll.find(selector, none[Bdoc]).cursor[D](readPreference).list(Int.MaxValue)

    def list[D: BSONDocumentReader](selector: Bdoc, limit: Int): Fu[List[D]] =
      coll.find(selector, none[Bdoc]).cursor[D]().list(limit = limit)

    def byId[D: BSONDocumentReader, I: BSONWriter](id: I): Fu[Option[D]] =
      one[D]($id(id))

    def byId[D: BSONDocumentReader](id: String): Fu[Option[D]] = one[D]($id(id))

    def byId[D: BSONDocumentReader](id: String, projection: Bdoc): Fu[Option[D]] = one[D]($id(id), projection)

    def byIds[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        readPreference: ReadPreference
    ): Fu[List[D]] =
      list[D]($inIds(ids), readPreference)

    def byIds[D: BSONDocumentReader](
        ids: Iterable[String],
        readPreference: ReadPreference = ReadPreference.primary
    ): Fu[List[D]] =
      byIds[D, String](ids, readPreference)

    def countSel(selector: coll.pack.Document): Fu[Int] =
      coll
        .count(
          selector = selector.some,
          limit = None,
          skip = 0,
          hint = None,
          readConcern = ReadConcern.Local
        )
        .dmap(_.toInt)

    def countAll: Fu[Long] =
      coll
        .count(
          selector = none,
          limit = None,
          skip = 0,
          hint = None,
          readConcern = ReadConcern.Local
        )

    def exists(selector: Bdoc): Fu[Boolean] = countSel(selector).dmap(0 !=)

    def byOrderedIds[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        projection: Option[Bdoc] = None,
        readPreference: ReadPreference = ReadPreference.primary
    )(docId: D => I): Fu[List[D]] =
      mapByOrderedIds[D, I](ids, projection, readPreference)(docId) map { m =>
        ids.view.flatMap(m.get).toList
      }

    def optionsByOrderedIds[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        projection: Option[Bdoc] = None,
        readPreference: ReadPreference = ReadPreference.primary
    )(docId: D => I): Fu[List[Option[D]]] =
      mapByOrderedIds[D, I](ids, projection, readPreference)(docId) map { m =>
        ids.view.map(m.get).toList
      }

    private def mapByOrderedIds[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        projection: Option[Bdoc],
        readPreference: ReadPreference
    )(docId: D => I): Fu[Map[I, D]] =
      projection
        .fold(coll.find($inIds(ids))) { proj =>
          coll.find($inIds(ids), proj.some)
        }
        .cursor[D](readPreference)
        .collect[List](Int.MaxValue)
        .map {
          _.view.map(u => docId(u) -> u).toMap
        }

    def idsMap[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        readPreference: ReadPreference = ReadPreference.primary
    )(docId: D => I): Fu[Map[I, D]] =
      byIds[D, I](ids, readPreference) map { docs =>
        docs.view.map(u => docId(u) -> u).toMap
      }

    def primitive[V: BSONReader](selector: Bdoc, field: String): Fu[List[V]] =
      coll
        .find(selector, $doc(field -> true).some)
        .cursor[Bdoc]()
        .list()
        .dmap {
          _ flatMap { _.getAsOpt[V](field) }
        }

    def primitive[V: BSONReader](selector: Bdoc, sort: Bdoc, field: String): Fu[List[V]] =
      coll
        .find(selector, $doc(field -> true).some)
        .sort(sort)
        .cursor[Bdoc]()
        .list()
        .dmap {
          _ flatMap { _.getAsOpt[V](field) }
        }

    def primitive[V: BSONReader](selector: Bdoc, sort: Bdoc, nb: Int, field: String): Fu[List[V]] =
      (nb > 0) ?? coll
        .find(selector, $doc(field -> true).some)
        .sort(sort)
        .cursor[Bdoc]()
        .list(nb)
        .dmap {
          _ flatMap { _.getAsOpt[V](field) }
        }

    def primitiveOne[V: BSONReader](selector: Bdoc, field: String): Fu[Option[V]] =
      coll
        .find(selector, $doc(field -> true).some)
        .one[Bdoc]
        .dmap {
          _ flatMap { _.getAsOpt[V](field) }
        }

    def primitiveOne[V: BSONReader](selector: Bdoc, sort: Bdoc, field: String): Fu[Option[V]] =
      coll
        .find(selector, $doc(field -> true).some)
        .sort(sort)
        .one[Bdoc]
        .dmap {
          _ flatMap { _.getAsOpt[V](field) }
        }

    def primitiveMap[I: BSONReader: BSONWriter, V](
        ids: Iterable[I],
        field: String,
        fieldExtractor: Bdoc => Option[V]
    ): Fu[Map[I, V]] =
      coll
        .find($inIds(ids), $doc(field -> true).some)
        .cursor[Bdoc]()
        .list()
        .dmap {
          _ flatMap { obj =>
            obj.getAsOpt[I]("_id") flatMap { id =>
              fieldExtractor(obj) map { id -> _ }
            }
          } toMap
        }

    def updateField[V: BSONWriter](selector: Bdoc, field: String, value: V) =
      coll.update.one(selector, $set(field -> value))

    def updateFieldUnchecked[V: BSONWriter](selector: Bdoc, field: String, value: V): Unit =
      coll
        .update(ordered = false, writeConcern = WriteConcern.Unacknowledged)
        .one(selector, $set(field -> value))
        .unit

    def incField(selector: Bdoc, field: String, value: Int = 1) =
      coll.update.one(selector, $inc(field -> value))

    def incFieldUnchecked(selector: Bdoc, field: String, value: Int = 1): Unit =
      coll
        .update(ordered = false, writeConcern = WriteConcern.Unacknowledged)
        .one(selector, $inc(field -> value))
        .unit

    def unsetField(selector: Bdoc, field: String, multi: Boolean = false) =
      coll.update.one(selector, $unset(field), multi = multi)

    def updateOrUnsetField[V: BSONWriter](selector: Bdoc, field: String, value: Option[V]): Fu[Int] =
      value match {
        case None    => unsetField(selector, field).dmap(_.n)
        case Some(v) => updateField(selector, field, v).dmap(_.n)
      }

    def fetchUpdate[D: BSONDocumentHandler](selector: Bdoc)(update: D => Bdoc): Funit =
      one[D](selector) flatMap {
        _ ?? { doc =>
          coll.update.one(selector, update(doc)).void
        }
      }

    def aggregateList(
        maxDocs: Int,
        readPreference: ReadPreference = ReadPreference.primary,
        allowDiskUse: Boolean = false
    )(
        f: coll.AggregationFramework => (coll.PipelineOperator, List[coll.PipelineOperator])
    )(implicit cp: CursorProducer[Bdoc]): Fu[List[Bdoc]] =
      coll
        .aggregateWith[Bdoc](
          allowDiskUse = allowDiskUse,
          readPreference = readPreference
        )(agg => {
          val nonEmpty = f(agg)
          nonEmpty._1 +: nonEmpty._2
        })
        .collect[List](maxDocs = maxDocs)

    def aggregateOne(
        readPreference: ReadPreference = ReadPreference.primary,
        allowDiskUse: Boolean = false
    )(
        f: coll.AggregationFramework => (coll.PipelineOperator, List[coll.PipelineOperator])
    )(implicit cp: CursorProducer[Bdoc]): Fu[Option[Bdoc]] =
      coll
        .aggregateWith[Bdoc](
          allowDiskUse = allowDiskUse,
          readPreference = readPreference
        )(agg => {
          val nonEmpty = f(agg)
          nonEmpty._1 +: nonEmpty._2
        })
        .collect[List](maxDocs = 1)
        .dmap(_.headOption) // .one[Bdoc] ?

    def aggregateExists(
        readPreference: ReadPreference = ReadPreference.primary,
        allowDiskUse: Boolean = false
    )(
        f: coll.AggregationFramework => (coll.PipelineOperator, List[coll.PipelineOperator])
    )(implicit cp: CursorProducer[Bdoc]): Fu[Boolean] =
      coll
        .aggregateWith[Bdoc](
          allowDiskUse = allowDiskUse,
          readPreference = readPreference
        )(agg => {
          val nonEmpty = f(agg)
          nonEmpty._1 +: nonEmpty._2
        })
        .headOption
        .dmap(_.isDefined)

    def distinctEasy[T, M[_] <: Iterable[_]](
        key: String,
        selector: coll.pack.Document,
        readPreference: ReadPreference = ReadPreference.primary
    )(implicit
        reader: coll.pack.NarrowValueReader[T],
        cbf: Factory[T, M[T]]
    ): Fu[M[T]] =
      coll.withReadPreference(readPreference).distinct(key, selector.some, ReadConcern.Local, None)

    def findAndUpdate[D: BSONDocumentReader](
        selector: coll.pack.Document,
        update: coll.pack.Document,
        fetchNewObject: Boolean = false,
        upsert: Boolean = false,
        sort: Option[coll.pack.Document] = None,
        fields: Option[coll.pack.Document] = None,
        @nowarn writeConcern: CWC = CWC.Acknowledged
    ): Fu[Option[D]] =
      coll.findAndUpdate(
        selector = selector,
        update = update,
        fetchNewObject = fetchNewObject,
        upsert = upsert,
        sort = sort,
        fields = fields,
        bypassDocumentValidation = false,
        writeConcern = writeConcern,
        maxTime = none,
        collation = none,
        arrayFilters = Seq.empty
      ) map {
        _.value flatMap implicitly[BSONDocumentReader[D]].readOpt
      }

    def findAndRemove[D: BSONDocumentReader](
        selector: coll.pack.Document,
        sort: Option[coll.pack.Document] = None,
        fields: Option[coll.pack.Document] = None,
        @nowarn writeConcern: CWC = CWC.Acknowledged
    ): Fu[Option[D]] =
      coll.findAndRemove(
        selector = selector,
        sort = sort,
        fields = fields,
        writeConcern = writeConcern,
        maxTime = none,
        collation = none,
        arrayFilters = Seq.empty
      ) map {
        _.value flatMap implicitly[BSONDocumentReader[D]].readOpt
      }
  }
}
