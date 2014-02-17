package lila.db

import play.api.libs.json._, Json.JsValueWrapper
import reactivemongo.api._
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.bson._

object Types extends Types
object Implicits extends Implicits

trait Types {

  type Coll = reactivemongo.api.collections.default.BSONCollection

  type QueryBuilder = GenericQueryBuilder[BSONDocument, BSONDocumentReader, BSONDocumentWriter]

  type Identified[ID] = { def id: ID }

  type Sort = Seq[(String, SortOrder)]

  type BSONWrites[A] = BSONWriter[A, BSONValue]
}

trait Implicits extends Types {

  implicit def docId[ID](doc: Identified[ID]): ID = doc.id

  def pimpQB(b: QueryBuilder) = new LilaPimpedQueryBuilder(b)

  // hack, this should be in reactivemongo
  implicit final class LilaPimpedQueryBuilder(b: QueryBuilder) {

    def sort(sorters: (String, SortOrder)*): QueryBuilder =
      if (sorters.size == 0) b
      else b sort {
        BSONDocument(
          (for (sorter ← sorters) yield sorter._1 -> BSONInteger(
            sorter._2 match {
              case SortOrder.Ascending  => 1
              case SortOrder.Descending => -1
            })).toStream)
      }

    def skip(nb: Int): QueryBuilder = b.options(b.options skip nb)

    def batch(nb: Int): QueryBuilder = b.options(b.options batchSize nb)

    def toList[A: BSONDocumentReader](limit: Option[Int]): Fu[List[A]] =
      limit.fold(b.cursor[A].collect[List]()) { l =>
        batch(l).cursor[A].collect[List](l)
      }

    def toListFlatten[A: Tube](limit: Option[Int]): Fu[List[A]] =
      toList[Option[A]](limit) map (_.flatten)
  }
}
