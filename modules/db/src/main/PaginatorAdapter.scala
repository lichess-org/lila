package lila.db
package paginator

import reactivemongo.api.*
import reactivemongo.api.bson.*
import scalalib.paginator.AdapterLike

import dsl.*

final class CachedAdapter[A](
    adapter: AdapterLike[A],
    val nbResults: Fu[Int]
)(using Executor)
    extends AdapterLike[A]:

  def slice(offset: Int, length: Int): Fu[Seq[A]] =
    adapter.slice(offset, length)

final class Adapter[A: BSONDocumentReader](
    collection: Coll,
    selector: Bdoc,
    projection: Option[Bdoc],
    sort: Bdoc,
    readPref: ReadPref = _.pri,
    hint: Option[Bdoc] = None
)(using Executor)
    extends AdapterLike[A]:

  def nbResults: Fu[Int] = collection.secondary.countSel(selector)

  def slice(offset: Int, length: Int): Fu[List[A]] =
    collection
      .find(selector, projection)
      .sort(sort)
      .skip(offset)
      .pipe: query =>
        hint.fold(query): h =>
          query.hint(collection.hint(h))
      .cursor[A](readPref)
      .list(length)

  def withNbResults(nb: Fu[Int]) = CachedAdapter(this, nb)
  def withLotsOfResults = withNbResults(fuccess(Int.MaxValue / 2))

final class StaticAdapter[A](results: Seq[A])(using Executor) extends AdapterLike[A]:

  def nbResults = fuccess(results.size)

  def slice(offset: Int, length: Int) = fuccess(results.drop(offset).take(length))
