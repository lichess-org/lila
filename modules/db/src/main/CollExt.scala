package lila.db

import scala.collection.Factory

import com.github.ghik.silencer.silent
import reactivemongo.api._
import reactivemongo.api.bson._
import reactivemongo.api.commands.{ WriteConcern => CWC, FindAndModifyCommand => FNM }

trait CollExt { self: dsl with QueryBuilderExt =>

  implicit final class ExtendColl(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

    def secondaryPreferred = coll withReadPreference ReadPreference.secondaryPreferred
    def secondary          = coll withReadPreference ReadPreference.secondary

    def ext = this

    def find(selector: Bdoc) = coll.find(selector, none)

    def find(selector: Bdoc, proj: Bdoc) = coll.find(selector, proj.some)

    def one[D: BSONDocumentReader](selector: Bdoc): Fu[Option[D]] =
      coll.find(selector, none).one[D]

    def one[D: BSONDocumentReader](selector: Bdoc, projection: Bdoc): Fu[Option[D]] =
      coll.find(selector, projection.some).one[D]

    def list[D: BSONDocumentReader](
        selector: Bdoc,
        readPreference: ReadPreference = ReadPreference.primary
    ): Fu[List[D]] =
      coll.find(selector, none).list[D](Int.MaxValue, readPreference = readPreference)

    def list[D: BSONDocumentReader](selector: Bdoc, limit: Int): Fu[List[D]] =
      coll.find(selector, none).list[D](limit = limit)

    def byId[D: BSONDocumentReader, I: BSONWriter](id: I): Fu[Option[D]] =
      one[D]($id(id))

    def byId[D: BSONDocumentReader](id: String): Fu[Option[D]]                   = one[D]($id(id))
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

    def countAll: Fu[Int] =
      coll
        .count(
          selector = none,
          limit = None,
          skip = 0,
          hint = None,
          readConcern = ReadConcern.Local
        )
        .dmap(_.toInt)

    def exists(selector: Bdoc): Fu[Boolean] = countSel(selector).dmap(0 !=)

    def byOrderedIds[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        projection: Option[Bdoc] = None,
        readPreference: ReadPreference = ReadPreference.primary
    )(docId: D => I): Fu[List[D]] =
      projection
        .fold(find($inIds(ids))) { proj =>
          find($inIds(ids), proj)
        }
        .cursor[D](readPreference = readPreference)
        .collect[List](Int.MaxValue, err = Cursor.FailOnError[List[D]]())
        .map { docs =>
          val docsMap: Map[I, D] = docs.view.map(u => docId(u) -> u).to(Map)
          ids.view.flatMap(docsMap.get).to(List)
        }

    def optionsByOrderedIds[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        readPreference: ReadPreference = ReadPreference.primary
    )(docId: D => I): Fu[List[Option[D]]] =
      byIds[D, I](ids, readPreference) map { docs =>
        val docsMap: Map[I, D] = docs.view.map(u => docId(u) -> u).to(Map)
        ids.view.map(docsMap.get).to(List)
      }

    def idsMap[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        readPreference: ReadPreference = ReadPreference.primary
    )(docId: D => I): Fu[Map[I, D]] =
      byIds[D, I](ids, readPreference) map { docs =>
        docs.view.map(u => docId(u) -> u).to(Map)
      }

    def primitive[V: BSONReader](selector: Bdoc, field: String): Fu[List[V]] =
      find(selector, $doc(field -> true))
        .list[Bdoc]()
        .dmap {
          _ flatMap { _.getAsOpt[V](field) }
        }

    def primitive[V: BSONReader](selector: Bdoc, sort: Bdoc, field: String): Fu[List[V]] =
      find(selector, $doc(field -> true))
        .sort(sort)
        .list[Bdoc]()
        .dmap {
          _ flatMap { _.getAsOpt[V](field) }
        }

    def primitive[V: BSONReader](selector: Bdoc, sort: Bdoc, nb: Int, field: String): Fu[List[V]] =
      find(selector, $doc(field -> true))
        .sort(sort)
        .list[Bdoc](nb)
        .dmap {
          _ flatMap { _.getAsOpt[V](field) }
        }

    def primitiveOne[V: BSONReader](selector: Bdoc, field: String): Fu[Option[V]] =
      find(selector, $doc(field -> true))
        .one[Bdoc]
        .dmap {
          _ flatMap { _.getAsOpt[V](field) }
        }

    def primitiveOne[V: BSONReader](selector: Bdoc, sort: Bdoc, field: String): Fu[Option[V]] =
      find(selector, $doc(field -> true))
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
      find($inIds(ids), $doc(field -> true))
        .list[Bdoc]()
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
      coll.update(false, writeConcern = WriteConcern.Unacknowledged).one(selector, $set(field -> value))

    def incField(selector: Bdoc, field: String, value: Int = 1) =
      coll.update.one(selector, $inc(field -> value))

    def incFieldUnchecked(selector: Bdoc, field: String, value: Int = 1): Unit =
      coll.update(false, writeConcern = WriteConcern.Unacknowledged).one(selector, $inc(field -> value))

    def unsetField(selector: Bdoc, field: String, multi: Boolean = false) =
      coll.update.one(selector, $unset(field), multi = multi)

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
        )(f)
        .collect[List](maxDocs = maxDocs, Cursor.FailOnError[List[Bdoc]]())

    def distinctEasy[T, M[_] <: Iterable[_]](
        key: String,
        selector: coll.pack.Document
    )(
        implicit
        reader: coll.pack.NarrowValueReader[T],
        cbf: Factory[T, M[T]]
    ): Fu[M[T]] =
      coll.distinct(key, selector.some, ReadConcern.Local, None)

    def findAndUpdate[S, T](
        selector: coll.pack.Document,
        update: coll.pack.Document,
        fetchNewObject: Boolean = false,
        upsert: Boolean = false,
        sort: Option[coll.pack.Document] = None,
        fields: Option[coll.pack.Document] = None,
        @silent writeConcern: CWC = CWC.Acknowledged
    ): Fu[FNM.Result[coll.pack.type]] =
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
      )
  }
}
