package lila.db
package api

import Types._

import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

object primitive extends primitive
trait primitive {

  def apply[A](q: JsObject, field: String, modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsValue ⇒ Option[A])(implicit coll: Coll): Fu[List[A]] =
    modifier(coll.genericQueryBuilder query q projection Json.obj(field -> true)).cursor.toList map (list ⇒ list map { obj ⇒
      extract(JsObjectReader.read(obj) \ field)
    } flatten)

  def one[A](q: JsObject, field: String, modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsValue ⇒ Option[A])(implicit coll: Coll): Fu[Option[A]] =
    modifier(coll.genericQueryBuilder query q projection Json.obj(field -> true)).one map (opt ⇒ opt map { obj ⇒
      extract(JsObjectReader.read(obj) \ field)
    } flatten)
}
