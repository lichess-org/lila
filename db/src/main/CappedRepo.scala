package lila.db

import Implicits._

import reactivemongo.api._
import reactivemongo.bson._

import play.modules.reactivemongo.Implicits.{ JsObjectWriter ⇒ _, _ }
import lila.db.PlayReactiveMongoPatch._

import play.api.libs.concurrent.Execution.Implicits._

abstract class CappedRepo[Doc](coll: ReactiveColl, json: JsonTube[Doc], max: Int) extends DbApi {

  val naturalOrder = sort desc "$natural" 

  val recent: Fu[List[Doc]] = (
    LilaPimpedQueryBuilder(coll.genericQueryBuilder query select.all).sort(naturalOrder) limit max
  ).cursor[Option[Doc]].toList map (_.flatten)

  private implicit val bsonDocumentReader = new BSONDocumentReader[Option[Doc]] {
    def read(bson: BSONDocument): Option[Doc] = json.fromMongo(JsObjectReader read bson).asOpt
  }
}
