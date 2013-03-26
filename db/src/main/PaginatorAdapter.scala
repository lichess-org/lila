package lila.db
package paginator

import Implicits._

import lila.common.paginator.AdapterLike
import play.api.libs.json._
import reactivemongo.api.SortOrder

final class Adapter[ID, A <: Identified[ID]](
    repo: Repo[ID, A],
    selector: JsObject,
    sort: Sort) extends AdapterLike[A] with api.Full {

  import repo.coll

  def nbResults: Fu[Int] = count(selector)

  def slice(offset: Int, length: Int): Fu[Seq[A]] = repo find {
    LilaPimpedQueryBuilder(query(selector)).sort(sort: _*) skip offset limit length
  }
}

final class CachedAdapter[A](
    adapter: AdapterLike[A],
    val nbResults: Fu[Int]) extends AdapterLike[A] {

  def slice(offset: Int, length: Int): Fu[Seq[A]] =
    adapter.slice(offset, length)
}
