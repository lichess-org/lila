package lila.db
package paginator

import Implicits._

import lila.common.paginator.AdapterLike
import play.api.libs.json._
import reactivemongo.api.SortOrder

final class Adapter[ID, A <: Identified[ID]](
    repo: Repo[ID, A],
    query: JsObject,
    sort: Seq[(String, SortOrder)]) extends AdapterLike[A] {

  def nbResults: Fu[Int] = repo count query

  def slice(offset: Int, length: Int): Fu[Seq[A]] = repo find {
    LilaPimpedQueryBuilder(repo query query).sort(sort: _*) skip offset limit length 
  }
}

final class CachedAdapter[ID, A <: Identified[ID]](
    adapter: Adapter[ID, A],
    val nbResults: Fu[Int]) extends AdapterLike[A] {

  def slice(offset: Int, length: Int) = adapter.slice(offset, length)
}
