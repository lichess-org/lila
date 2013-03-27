package lila.db

import Implicits._

import reactivemongo.api._
import reactivemongo.bson._

import play.modules.reactivemongo.Implicits._

import play.api.libs.concurrent.Execution.Implicits._

abstract class CappedRepo[Doc](
  implicit coll: ReactiveColl, 
  json: JsonTube[Doc], 
  max: Int) extends api.Full {

  val naturalOrder = sort desc "$natural" 

  val recent: Fu[List[Doc]] = (
    LilaPimpedQueryBuilder(query.all).sort(naturalOrder) limit max
  ).cursor[Option[Doc]].toList map (_.flatten)

  private implicit val bsonDocumentReader = new BSONDocumentReader[Option[Doc]] {
    def read(bson: BSONDocument): Option[Doc] = json.fromMongo(JsObjectReader read bson).asOpt
  }
}
