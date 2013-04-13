package lila.db
package api

import Types._

import reactivemongo.bson._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import play.api.libs.json._

object $projection {

  def apply[A: InColl, B](
    q: JsObject,
    fields: Seq[String],
    modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsObject ⇒ Option[B]): Fu[List[B]] =
    modifier(implicitly[InColl[A]].coll.genericQueryBuilder query q projection projector(fields)).cursor.toList map (list ⇒ list map { obj ⇒
      extract(JsObjectReader read obj)
    } flatten)

  def one[A:InColl, B](
    q: JsObject,
    fields: Seq[String],
    modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsObject ⇒ Option[B]): Fu[Option[B]] =
    modifier(implicitly[InColl[A]].coll.genericQueryBuilder query q projection projector(fields)).one map (opt ⇒ opt map { obj ⇒
      extract(JsObjectReader read obj)
    } flatten)

  private def projector(fields: Seq[String]): JsObject = Json obj {
    (fields map (_ -> Json.toJsFieldJsValueWrapper(1))): _*
  }
}
