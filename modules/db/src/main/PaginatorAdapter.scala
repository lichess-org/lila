package lila.db
package paginator

import Implicits._
import api._

import lila.common.paginator.AdapterLike
import play.api.libs.json._
import reactivemongo.api.SortOrder

final class Adapter[A: TubeInColl](
    selector: JsObject,
    sort: Sort) extends AdapterLike[A] {

  def nbResults: Fu[Int] = $count(selector)

  def slice(offset: Int, length: Int): Fu[Seq[A]] = $find {
    pimpQB($query(selector)).sort(sort: _*) skip offset limit length
  }
}

final class CachedAdapter[A](
    adapter: AdapterLike[A],
    val nbResults: Fu[Int]) extends AdapterLike[A] {

  def slice(offset: Int, length: Int): Fu[Seq[A]] =
    adapter.slice(offset, length)
}
