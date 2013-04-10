package lila.db
package api

import Types._

import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._
import play.api.libs.json._

object $primitive {

  def apply[A: InColl, B](
    query: JsObject,
    field: String,
    modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsValue ⇒ Option[B]): Fu[List[B]] =
    modifier {
      implicitly[InColl[A]].coll
        .genericQueryBuilder
        .query(query)
        .projection(Json.obj(field -> true))
    }.cursor.toList map2 { (obj: BSONDocument) ⇒
      extract(JsObjectReader.read(obj) \ field)
    } map (_.flatten)

  def one[A: InColl, B](
    query: JsObject,
    field: String,
    modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsValue ⇒ Option[B]): Fu[Option[B]] =
    modifier {
      implicitly[InColl[A]].coll
        .genericQueryBuilder
        .query(query)
        .projection(Json.obj(field -> true))
    }.one map2 { (obj: BSONDocument) ⇒
      extract(JsObjectReader.read(obj) \ field)
    } map (_.flatten)
}
