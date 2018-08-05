package lila.db
package paginator

import dsl._
import reactivemongo.api._
import reactivemongo.bson._

import lila.common.paginator.AdapterLike

final class CachedAdapter[A](
    adapter: AdapterLike[A],
    val nbResults: Fu[Int]
) extends AdapterLike[A] {

  def slice(offset: Int, length: Int): Fu[Seq[A]] =
    adapter.slice(offset, length)
}

final class Adapter[A: BSONDocumentReader](
    collection: Coll,
    selector: BSONDocument,
    projection: BSONDocument,
    sort: BSONDocument,
    readPreference: ReadPreference = ReadPreference.primary
) extends AdapterLike[A] {
  def nbResults: Fu[Int] = collection.countSel(selector, readPreference)

  def slice(offset: Int, length: Int): Fu[List[A]] =
    collection.find(selector, Some(projection))
      .sort(sort).skip(offset).cursor[A](readPreference).list(length)
}
