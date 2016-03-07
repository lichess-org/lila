package lila.db

import reactivemongo.api.BSONSerializationPack
// import reactivemongo.api.commands._
import reactivemongo.api.commands.bson._
import reactivemongo.api.commands.{ DistinctCommand => DistC, DefaultWriteResult, LastError, ResolvedCollectionCommand }
import reactivemongo.bson._

object LowLevelDistinct {

  private implicit def DistinctWriter = BSONDistinctCommandImplicits.DistinctWriter
  private implicit def DistinctResultReader = BSONDistinctCommandImplicits.DistinctResultReader

  /**
   * Returns the distinct values for a specified field across a single collection and returns the results in an array.
   * @param key the field for which to return distinct values
   * @param selector the query selector that specifies the documents from which to retrieve the distinct values.
   */
  def apply(coll: Types.Coll)(key: String, selector: Option[BSONDocument] = None): Fu[Traversable[BSONValue]] =
    coll.runCommand(DistinctCommand.Distinct(keyString = key, query = selector)).map(_.values)

  private val DistinctCommand = new DistC[BSONSerializationPack.type] {
    val pack = BSONSerializationPack
  }

  private object BSONDistinctCommandImplicits {
    import DistinctCommand._

    implicit object DistinctWriter extends BSONDocumentWriter[ResolvedCollectionCommand[Distinct]] {
      def write(distinct: ResolvedCollectionCommand[Distinct]): BSONDocument =
        BSONDocument(
          "distinct" -> distinct.collection,
          "key" -> distinct.command.keyString,
          "query" -> distinct.command.query)
    }

    implicit object DistinctResultReader extends DealingWithGenericCommandErrorsReader[DistinctResult] {
      def readResult(doc: BSONDocument): DistinctResult =
        DistinctResult(doc.getAs[BSONArray]("values").fold[Stream[BSONValue]](Stream.empty)(_.values))
    }
  }
}
