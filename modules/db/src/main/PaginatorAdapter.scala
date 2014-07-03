package lila.db
package paginator

import api._
import Implicits._
import play.api.libs.json._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.SortOrder
import reactivemongo.api.{ QueryOpts, SortOrder }
import reactivemongo.bson._
import reactivemongo.core.commands.Count

import lila.common.paginator.AdapterLike

final class Adapter[A: TubeInColl](
    selector: JsObject,
    sort: Sort) extends AdapterLike[A] {

  def nbResults: Fu[Int] = $count(selector)

  def slice(offset: Int, length: Int): Fu[Seq[A]] = $find(
    pimpQB($query(selector)).sort(sort: _*) skip offset,
    length
  )
}

final class CachedAdapter[A](
    adapter: AdapterLike[A],
    val nbResults: Fu[Int]) extends AdapterLike[A] {

  def slice(offset: Int, length: Int): Fu[Seq[A]] =
    adapter.slice(offset, length)
}

final class BSONAdapter[A: BSONDocumentReader](
    collection: BSONCollection,
    selector: BSONDocument,
    sort: BSONDocument) extends AdapterLike[A] {

  def nbResults: Fu[Int] =
    collection.db command Count(collection.name, Some(selector))

  def slice(offset: Int, length: Int): Fu[Seq[A]] =
    collection.find(selector)
      .sort(sort)
      .copy(options = QueryOpts(skipN = offset))
      .cursor[A]
      .collect[List](length)
}
