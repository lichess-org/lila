package lila.db

import reactivemongo.api._
import reactivemongo.api.commands._
import reactivemongo.api.bson.collection.BSONSerializationPack

import dsl._

trait MapReduceCommand[P <: SerializationPack] extends ImplicitCommandHelpers[P] {

  case class MapReduce(
      collection: String,
      query: pack.Document,
      sort: pack.Document,
      map: String,
      reduce: String,
      jsMode: Boolean
  ) extends CollectionCommand
    with CommandWithPack[pack.type] with CommandWithResult[MapReduceResult]

  case class MapReduceResult(results: List[pack.Document])
}

object BSONMapReduceCommand extends MapReduceCommand[BSONSerializationPack.type] {
  val pack = BSONSerializationPack

  object Implicits {
    import reactivemongo.api.commands.ResolvedCollectionCommand
    import reactivemongo.bson._

    implicit object BSONWriter
      extends BSONDocumentWriter[ResolvedCollectionCommand[MapReduce]] {
      // type `MapReduce` inherited from the specification `MapReduceCommand` trait

      def write(mapReduce: ResolvedCollectionCommand[MapReduce]) = {
        val cmd: MapReduce = mapReduce.command
        $doc(
          "mapreduce" -> cmd.collection,
          "query" -> cmd.query,
          "sort" -> cmd.sort,
          "map" -> cmd.map,
          "reduce" -> cmd.reduce,
          "jsMode" -> cmd.jsMode
        )
      }
    }

    implicit object BSONReader extends BSONDocumentReader[MapReduceResult] {
      // type `MapReduceResult` inherited from the `MapReduceCommand` trait
      def read(result: BSONDocument): MapReduceResult = (for {
        results <- result.getAs[List[BSONDocument]]("results")
      } yield MapReduceResult(results)).get
    }
  }
}
