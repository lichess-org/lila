package lidraughts.db
package paginator

import dsl._
import reactivemongo.api._
import reactivemongo.bson._

import lidraughts.common.paginator.AdapterLike

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

final class MapReduceAdapter[A: BSONDocumentReader](
    collection: Coll,
    selector: Bdoc,
    runCommand: RunCommand,
    command: MapReduceAdapter.Command,
    readPreference: ReadPreference = ReadPreference.primary
) extends AdapterLike[A] {

  def nbResults: Fu[Int] = collection.countSel(selector, readPreference)

  def slice(offset: Int, length: Int): Fu[List[A]] =
    runCommand(
      command(offset) ++ $doc("limit" -> (offset + length)),
      readPreference
    ) map { res =>
        res.getAs[List[Bdoc]]("results").??(_ map implicitly[BSONDocumentReader[A]].read)
      }
}

object MapReduceAdapter {
  // offset -> doc
  type Command = Int => Bdoc
}
