package lila.db
package paginator

import dsl._
import reactivemongo.api._
import reactivemongo.bson._

import lila.common.paginator.AdapterLike

final class CachedAdapter[A](
    adapter: AdapterLike[A],
    val nbResults: Fu[Int]
) extends AdapterLike[A] {

  def slice(offset: Int, length: Int): Fu[Seq[A]] =
    adapter.slice(offset, length)
}

final class Adapter[A: BSONDocumentReader](
    collection: Coll,
    selector: Bdoc,
    projection: Bdoc,
    sort: Bdoc,
    readPreference: ReadPreference = ReadPreference.primary
) extends AdapterLike[A] {

  def nbResults: Fu[Int] = collection.countSel(selector, readPreference)

  def slice(offset: Int, length: Int): Fu[List[A]] =
    collection.find(selector, projection)
      .sort(sort)
      .skip(offset)
      .list[A](length, readPreference)
}

/*
 * because mongodb mapReduce doesn't support `skip`, slice requires to queries.
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
) extends AdapterLike[A] {

  def nbResults: Fu[Int] = collection.countSel(selector, readPreference)

  def slice(offset: Int, length: Int): Fu[List[A]] =
    collection.find(selector, $id(true))
      .sort(sort)
      .skip(offset)
      .list[Bdoc](length, readPreference)
      .dmap { _ flatMap { _.getAs[BSONString]("_id") } }
      .flatMap { ids =>
        runCommand(
          $doc(
            "mapreduce" -> collection.name,
            "query" -> $inIds(ids),
            "sort" -> sort,
            "out" -> $doc("inline" -> true)
          ) ++ command,
          readPreference
        ) map { res =>
            res.getAs[List[Bdoc]]("results").??(_ map implicitly[BSONDocumentReader[A]].read)
          }
      }
}
