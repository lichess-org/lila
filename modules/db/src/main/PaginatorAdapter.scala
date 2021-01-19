package lila.db
package paginator

import dsl._
import reactivemongo.api._
import reactivemongo.api.bson._
import scala.concurrent.ExecutionContext
import scala.util.chaining._

import lila.common.paginator.AdapterLike

final class CachedAdapter[A](
    adapter: AdapterLike[A],
    val nbResults: Fu[Int]
)(implicit ec: ExecutionContext)
    extends AdapterLike[A] {

  def slice(offset: Int, length: Int): Fu[Seq[A]] =
    adapter.slice(offset, length)
}

final class Adapter[A: BSONDocumentReader](
    collection: Coll,
    selector: Bdoc,
    projection: Option[Bdoc],
    sort: Bdoc,
    readPreference: ReadPreference = ReadPreference.primary,
    hint: Option[Bdoc] = None
)(implicit ec: ExecutionContext)
    extends AdapterLike[A] {

  def nbResults: Fu[Int] = collection.secondaryPreferred.countSel(selector)

  def slice(offset: Int, length: Int): Fu[List[A]] =
    collection
      .find(selector, projection)
      .sort(sort)
      .skip(offset)
      .pipe { query =>
        hint match {
          case None    => query
          case Some(h) => query.hint(collection hint h)
        }
      }
      .cursor[A](readPreference)
      .list(length)
}

final class StaticAdapter[A](results: Seq[A])(implicit ec: ExecutionContext) extends AdapterLike[A] {

  def nbResults = fuccess(results.size)

  def slice(offset: Int, length: Int) = fuccess(results drop offset take length)
}
