package lila.db

import play.api.libs.json._, Json.JsValueWrapper
import reactivemongo.api._
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.bson._
import ornicar.scalalib.Zero

object Types extends Types
object Implicits extends Implicits

trait Types {
  type Coll = reactivemongo.api.collections.bson.BSONCollection

  type QueryBuilder = GenericQueryBuilder[BSONSerializationPack.type]

  type Identified[ID] = { def id: ID }

  type Sort = Seq[(String, api.SortOrder)]

  type BSONValueReader[A] = BSONReader[_ <: BSONValue, A]
  type BSONValueHandler[A] = BSONHandler[_ <: BSONValue, A]
}

trait Implicits extends Types {

  implicit val LilaBSONDocumentZero: Zero[BSONDocument] =
    Zero.instance(BSONDocument())

  implicit def docId[ID](doc: Identified[ID]): ID = doc.id

  def pimpQB(b: QueryBuilder) = new LilaPimpedQueryBuilder(b)

  // hack, this should be in reactivemongo
  implicit final class LilaPimpedQueryBuilder(b: QueryBuilder) {

    def sort(sorters: (String, api.SortOrder)*): QueryBuilder =
      if (sorters.size == 0) b
      else b sort {
        BSONDocument(
          (for (sorter ← sorters) yield sorter._1 -> BSONInteger(
            sorter._2 match {
              case api.SortOrder.Ascending  => 1
              case api.SortOrder.Descending => -1
            })).toStream)
      }

    def skip(nb: Int): QueryBuilder = b.options(b.options skip nb)

    def batch(nb: Int): QueryBuilder = b.options(b.options batchSize nb)

    def toList[A: BSONDocumentReader](limit: Option[Int], readPreference: ReadPreference = ReadPreference.primary): Fu[List[A]] =
      limit.fold(b.cursor[A](readPreference = readPreference).collect[List]()) { l =>
        batch(l).cursor[A](readPreference = readPreference).collect[List](l)
      }

    def toListFlatten[A: Tube](limit: Option[Int]): Fu[List[A]] =
      toList[Option[A]](limit) map (_.flatten)
  }
}
