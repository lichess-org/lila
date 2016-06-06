package lila.db

import reactivemongo.api._
import reactivemongo.bson._

trait CollExt { self: dsl with QueryBuilderExt =>

  final implicit class ExtendColl(coll: Coll) {

    def uno[D: BSONDocumentReader](selector: BSONDocument): Fu[Option[D]] =
      coll.find(selector).uno[D]

    def list[D: BSONDocumentReader](selector: BSONDocument): Fu[List[D]] =
      coll.find(selector).list[D]()

    def list[D: BSONDocumentReader](selector: BSONDocument, max: Int): Fu[List[D]] =
      coll.find(selector).list[D](max)

    def byId[D: BSONDocumentReader, I: BSONValueWriter](id: I): Fu[Option[D]] =
      uno[D]($id(id))

    def byId[D: BSONDocumentReader](id: String): Fu[Option[D]] = uno[D]($id(id))

    def byId[D: BSONDocumentReader](id: Int): Fu[Option[D]] = uno[D]($id(id))

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
        .list[BSONDocument]()
        .map {
          _ flatMap { _.getAs[V](field) }
        }

    def primitiveOne[V: BSONValueReader](selector: BSONDocument, field: String): Fu[Option[V]] =
      coll.find(selector, $doc(field -> true))
        .uno[BSONDocument]
        .map {
          _ flatMap { _.getAs[V](field) }
        }

    def primitiveOne[V: BSONValueReader](selector: BSONDocument, sort: BSONDocument, field: String): Fu[Option[V]] =
      coll.find(selector, $doc(field -> true))
        .sort(sort)
        .uno[BSONDocument]
        .map {
          _ flatMap { _.getAs[V](field) }
        }

    def updateField[V: BSONValueWriter](selector: BSONDocument, field: String, value: V) =
      coll.update(selector, $set(field -> value))

    def updateFieldUnchecked[V: BSONValueWriter](selector: BSONDocument, field: String, value: V) =
      coll.uncheckedUpdate(selector, $set(field -> value))

    def incField(selector: BSONDocument, field: String, value: Int = 1) =
      coll.update(selector, $inc(field -> value))

    def incFieldUnchecked(selector: BSONDocument, field: String, value: Int = 1) =
      coll.uncheckedUpdate(selector, $inc(field -> value))

    def unsetField(selector: BSONDocument, field: String) =
      coll.update(selector, $unset(field))

    def fetchUpdate[D: BSONDocumentHandler](selector: BSONDocument)(update: D => BSONDocument): Funit =
      uno[D](selector) flatMap {
        _ ?? { doc =>
          coll.update(selector, update(doc)).void
        }
      }
  }
}
