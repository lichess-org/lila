package lila.db
package paginator

import dsl._
import reactivemongo.api.collections.bson.BSONCollection
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
    collection: BSONCollection,
    selector: BSONDocument,
    projection: BSONDocument,
    sort: BSONDocument,
    readPreference: ReadPreference = ReadPreference.primary
) extends AdapterLike[A] {

  def nbResults: Fu[Int] = collection.count(Some(selector))

  def slice(offset: Int, length: Int): Fu[Seq[A]] =
    collection.find(selector, projection)
      .sort(sort)
      .skip(offset)
      .cursor[A](readPreference = readPreference)
      .gather[List](length)
}
