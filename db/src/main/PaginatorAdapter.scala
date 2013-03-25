package lila.db
package paginator

import Implicits._

import lila.common.paginator.AdapterLike
import play.api.libs.json._
import reactivemongo.api.SortOrder

final class Adapter[ID, A <: Identified[ID]](
    repo: Repo[ID, A],
    query: JsObject,
    sort: Sort) extends AdapterLike[A] {

  def nbResults: Fu[Int] = repo count query

  def slice(offset: Int, length: Int): Fu[Seq[A]] = repo find {
    LilaPimpedQueryBuilder(repo query query).sort(sort: _*) skip offset limit length 
  }
}

final class CachedAdapter[A](
    adapter: AdapterLike[A],
    val nbResults: Fu[Int]) extends AdapterLike[A] {

  def slice(offset: Int, length: Int): Fu[Seq[A]] = 
    adapter.slice(offset, length)
}
