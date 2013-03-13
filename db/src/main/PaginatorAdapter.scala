package lila.db
package paginator

import lila.common.paginator.AdapterLike
import play.api.libs.json._
import reactivemongo.api.SortOrder

final class Adapter[A <: WithStringId](
    repo: Repo[A],
    query: JsObject,
    sort: Seq[(String, SortOrder)]) extends AdapterLike[A] {

  def nbResults: Fu[Int] = repo count query

  def slice(offset: Int, length: Int): Fu[Seq[A]] = repo find {
    richerQueryBuilder(repo query query).sort(sort: _*) skip offset limit length 
  }
}

final class CachedAdapter[A <: WithStringId](
    adapter: Adapter[A],
    val nbResults: Fu[Int]) extends AdapterLike[A] {

  def slice(offset: Int, length: Int) = adapter.slice(offset, length)
}
