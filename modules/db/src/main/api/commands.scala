package lila.db
package api

import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.core.commands._
import reactivemongo.api.{ SerializationPack, BSONSerializationPack }
import reactivemongo.api.commands.{
  CollectionCommand, CommandWithPack, CommandWithResult, ImplicitCommandHelpers
}

trait MapReduceCommand[P <: SerializationPack]
    extends ImplicitCommandHelpers[P] {

  case class MapReduce(
    mapFunction: JSFunction,
    reduceFunction: JSFunction,
    query: Option[pack.Document] = None,
    sort: Option[pack.Document] = None,
    limit: Option[Int] = None,
    finalizeFunction: Option[JSFunction] = None,
    scope: Option[String] = None,
    verbose: Boolean = false)
      extends CollectionCommand with CommandWithPack[pack.type] with CommandWithResult[pack.Document]

}

object BSONMapReduceCommand
    extends MapReduceCommand[BSONSerializationPack.type] {
  val pack = BSONSerializationPack
}

object BSONMapReduceCommandImplicits {
  import reactivemongo.api.commands.ResolvedCollectionCommand
  import reactivemongo.bson.BSONDocument
  import BSONMapReduceCommand._

  implicit object MapReduceWriter
      extends BSONDocumentWriter[ResolvedCollectionCommand[MapReduce]] {
    def write(mapr: ResolvedCollectionCommand[MapReduce]): BSONDocument = {
      val cmd = mapr.command
      BSONDocument(
        "mapReduce" -> BSONString(mapr.collection),
        "map" -> BSONString(cmd.mapFunction),
        "reduce" -> BSONString(cmd.reduceFunction),
        "out" -> BSONDocument("inline" -> true),
        "query" -> cmd.query,
        "sort" -> cmd.sort,
        "limit" -> cmd.limit.map(x => BSONInteger(x)),
        "finalize" -> cmd.finalizeFunction.map(x => BSONString(x)),
        "scope" -> cmd.scope.map(x => BSONString(x)),
        "verbose" -> BSONBoolean(cmd.verbose)
      )
    }
  }
}
