package lila.db

import dsl._

import reactivemongo.api._
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.bson._

trait CollExt {

  final implicit class Ext(val coll: Coll) {

    def one[D: BSONDocumentReader](selector: BSONDocument): Fu[Option[D]] =
      coll.find(selector).one[D]

    def list[D: BSONDocumentReader](selector: BSONDocument): Fu[List[D]] =
      coll.find(selector).cursor[D]().collect[List]()

    def byId[D: BSONDocumentReader, I: BSONValueWriter](id: I): Fu[Option[D]] =
      one[D]($id(id))

    def byId[D: BSONDocumentReader](id: String): Fu[Option[D]] =
      one[D]($id(id))

    def byIds[D: BSONDocumentReader, I: BSONValueWriter](ids: Iterable[I]): Fu[List[D]] =
      list[D]($inIds(ids))

    def byIds[D: BSONDocumentReader](ids: Iterable[String]): Fu[List[D]] =
      byIds[D, String](ids)

    def countSel(selector: BSONDocument): Fu[Int] = coll count selector.some

    def exists(selector: BSONDocument): Fu[Boolean] = countSel(selector).map(0!=)

    def byOrderedIds[D: BSONDocumentReader](ids: Iterable[String])(docId: D => String): Fu[List[D]] =
      byIds[D](ids) map { docs =>
        val docsMap = docs.map(u => docId(u) -> u).toMap
        ids.flatMap(docsMap.get).toList
      }
    // def byOrderedIds[A <: Identified[String]: TubeInColl](ids: Iterable[String]): Fu[List[A]] =
    //   byOrderedIds[String, A](ids)

    def optionsByOrderedIds[D: BSONDocumentReader](ids: Iterable[String])(docId: D => String): Fu[List[Option[D]]] =
      byIds[D](ids) map { docs =>
        val docsMap = docs.map(u => docId(u) -> u).toMap
        ids.map(docsMap.get).toList
      }

    def primitive[V: BSONValueReader](selector: BSONDocument, field: String): Fu[List[V]] =
      coll.find(selector, $doc(field -> true))
        .cursor[BSONDocument]().collect[List]()
        .map {
          _ flatMap { _.getAs[V](field) }
        }

    def primitiveOne[V: BSONValueReader](selector: BSONDocument, field: String): Fu[Option[V]] =
      coll.find(selector, $doc(field -> true))
        .one[BSONDocument]
        .map {
          _ flatMap { _.getAs[V](field) }
        }

    def primitiveOne[V: BSONValueReader](selector: BSONDocument, sort: BSONDocument, field: String): Fu[Option[V]] =
      coll.find(selector, $doc(field -> true))
        .sort(sort)
        .one[BSONDocument]
        .map {
          _ flatMap { _.getAs[V](field) }
        }

    def updateField[V: BSONValueWriter](selector: BSONDocument, field: String, value: V) =
      coll.update(selector, $doc(field -> value))

    def updateFieldUnchecked[V: BSONValueWriter](selector: BSONDocument, field: String, value: V) =
      coll.uncheckedUpdate(selector, $doc(field -> value))

    def fetchUpdate[D: BSONDocumentHandler](selector: BSONDocument)(update: D => BSONDocument): Funit =
      one[D](selector) flatMap {
        _ ?? { doc =>
          coll.update(selector, update(doc)).void
        }
      }
  }
}

object CollExt extends CollExt
