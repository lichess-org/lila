package lila.db
package paginator

import dsl.*
import reactivemongo.api.*
import reactivemongo.api.bson.*
import scala.util.chaining.*

import lila.common.paginator.AdapterLike

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
    readPreference: ReadPreference = ReadPreference.primary,
    hint: Option[Bdoc] = None
)(using Executor)
    extends AdapterLike[A]:

  def nbResults: Fu[Int] = collection.secondaryPreferred.countSel(selector)

  def slice(offset: Int, length: Int): Fu[List[A]] =
    collection
      .find(selector, projection)
      .sort(sort)
      .skip(offset)
      .pipe { query =>
        hint match
          case None    => query
          case Some(h) => query.hint(collection hint h)
      }
      .cursor[A](readPreference)
      .list(length)

  def withNbResults(nb: Fu[Int]) = new CachedAdapter(this, nb)

final class StaticAdapter[A](results: Seq[A])(using Executor) extends AdapterLike[A]:

  def nbResults = fuccess(results.size)

  def slice(offset: Int, length: Int) = fuccess(results drop offset take length)
