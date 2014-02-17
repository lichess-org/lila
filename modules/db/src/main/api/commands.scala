package lila.db
package api

import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.core.commands._

/**
 * MapReduce Command.
 *
 * Actually only produces inline output
 */
case class MapReduce(
    collectionName: String,
    mapFunction: JSFunction,
    reduceFunction: JSFunction,
    query: Option[BSONDocument] = None,
    sort: Option[BSONDocument] = None,
    limit: Option[Int] = None,
    finalizeFunction: Option[JSFunction] = None,
    scope: Option[String] = None,
    verbose: Boolean = false) extends Command[BSONDocument] {

  override def makeDocuments = BSONDocument(
    "mapReduce" -> BSONString(collectionName),
    "map" -> BSONString(mapFunction),
    "reduce" -> BSONString(reduceFunction),
    "out" -> BSONDocument("inline" -> true),
    "query" -> query,
    "sort" -> sort,
    "limit" -> limit.map(x => BSONInteger(x)),
    "finalize" -> finalizeFunction.map(x => BSONString(x)),
    "scope" -> scope.map(x => BSONString(x)),
    "verbose" -> BSONBoolean(verbose)
  )

  val ResultMaker = new BSONCommandResultMaker[BSONDocument] {
    def apply(document: BSONDocument) = Right(document)
  }
}
