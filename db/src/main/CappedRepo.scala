package lila.db

import Implicits._

import reactivemongo.api._
import reactivemongo.bson._

import play.modules.reactivemongo.Implicits._

import play.api.libs.concurrent.Execution.Implicits._

abstract class CappedRepo[Doc] extends api.Full {

    def max: Int
    def json: Tube[Doc]
    implicit def coll: Coll

  val naturalOrder = sort desc "$natural" 

  val recent: Fu[List[Doc]] = (
    LilaPimpedQueryBuilder(query.all).sort(naturalOrder) limit max
  ).cursor[Option[Doc]].toList map (_.flatten)

  implicit val bsonReader = new BSONDocumentReader[Option[Doc]] {
    def read(bson: BSONDocument): Option[Doc] = json.fromMongo(JsObjectReader read bson).asOpt
  }
}
