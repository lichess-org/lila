package lila.db
package api

import Implicits._
import play.api.libs.json._
import reactivemongo.bson._

object $primitive {
  import play.modules.reactivemongo.json._

  def apply[A: InColl, B](
    query: JsObject,
    field: String,
    modifier: QueryBuilder => QueryBuilder = identity,
    max: Option[Int] = None,
    hint: BSONDocument = BSONDocument())(extract: JsValue => Option[B]): Fu[List[B]] =
    modifier {
      implicitly[InColl[A]].coll
        .genericQueryBuilder
        .query(query)
        .hint(hint)
        .projection(Json.obj(field -> true))
    } toList[BSONDocument] max map2 { (obj: BSONDocument) =>
      extract(JsObjectReader.read(obj) \ field get)
    } map (_.flatten)

  def one[A: InColl, B](
    query: JsObject,
    field: String,
    modifier: QueryBuilder => QueryBuilder = identity)(extract: JsValue => Option[B]): Fu[Option[B]] =
    modifier {
      implicitly[InColl[A]].coll
        .genericQueryBuilder
        .query(query)
        .projection(Json.obj(field -> true))
    }.one[BSONDocument] map2 { (obj: BSONDocument) =>
      (JsObjectReader.read(obj) \ field).toOption flatMap extract
    } map (_.flatten)
}
