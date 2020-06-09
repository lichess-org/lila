package lila.db
package paginator

import dsl._
import reactivemongo.api._
import reactivemongo.api.bson._
import scala.util.chaining._

import lila.common.paginator.AdapterLike

final class CachedAdapter[A](
    adapter: AdapterLike[A],
    val nbResults: Fu[Long]
)(implicit ec: scala.concurrent.ExecutionContext)
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
)(implicit ec: scala.concurrent.ExecutionContext)
    extends AdapterLike[A] {

  def nbResults: Fu[Long] = collection.secondaryPreferred.countSel(selector).dmap(_.toLong)

  def slice(offset: Int, length: Int): Fu[List[A]] =
    collection
      .find(selector, projection)
      .sort(sort)
      .skip(offset)
      .pipe { query =>
        hint match {
          case None    => query
          case Some(h) => query hint h
        }
      }
      .list[A](length, readPreference)
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
)(implicit ec: scala.concurrent.ExecutionContext)
    extends AdapterLike[A] {

  def nbResults: Fu[Long] = collection.secondaryPreferred.countSel(selector).dmap(_.toLong)

  def slice(offset: Int, length: Int): Fu[List[A]] =
    collection
      .find(selector, $id(true).some)
      .sort(sort)
      .skip(offset)
      .list[Bdoc](length, readPreference)
      .dmap { _ flatMap { _.getAsOpt[BSONString]("_id") } }
      .flatMap { ids =>
        runCommand(
          $doc(
            "mapreduce" -> collection.name,
            "query"     -> $inIds(ids),
            "sort"      -> sort,
            "out"       -> $doc("inline" -> true)
          ) ++ command,
          readPreference
        ) map { res =>
          res.getAsOpt[List[Bdoc]]("results").??(_ flatMap implicitly[BSONDocumentReader[A]].readOpt)
        }
      }
}
