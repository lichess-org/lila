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

/*
 * because mongodb mapReduce doesn't support `skip`, slice requires two queries.
 * The first one gets the IDs with `skip`.
 * The second one runs the mapReduce on these IDs.
 * This avoid running mapReduce on many unnecessary docs.
 * NOTE: Requires string ID.
 */
final class MapReduceAdapter[A: BSONDocumentReader](
    collection: Coll,
    selector: Bdoc,
    sort: Bdoc,
    runCommand: RunCommand,
    command: Bdoc,
    readPreference: ReadPreference = ReadPreference.primary
)(implicit ec: ExecutionContext)
    extends AdapterLike[A] {

  def nbResults: Fu[Int] = collection.secondaryPreferred.countSel(selector)

  def slice(offset: Int, length: Int): Fu[List[A]] =
    collection
      .find(selector, $id(true).some)
      .sort(sort)
      .skip(offset)
      .cursor[Bdoc](readPreference)
      .list(length)
      .dmap { _ flatMap { _.getAsOpt[BSONString]("_id") } }
      .flatMap { ids =>
        runCommand(
          $doc(
            "mapreduce" -> collection.name,
            "query"     -> $inIds(ids),
            "sort"      -> sort,
            "out"       -> $doc("inline" -> 1)
          ) ++ command,
          readPreference
        ) map { res =>
          res.getAsOpt[List[Bdoc]]("results").??(_ flatMap implicitly[BSONDocumentReader[A]].readOpt)
        }
      }
}

final class StaticAdapter[A](results: Seq[A])(implicit ec: ExecutionContext) extends AdapterLike[A] {

  def nbResults = fuccess(results.size)

  def slice(offset: Int, length: Int) = fuccess(results drop offset take length)
}
