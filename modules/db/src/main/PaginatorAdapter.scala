package lila.db
package paginator

import api._
import Implicits._
import play.api.libs.json._
import reactivemongo.api.SortOrder

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
