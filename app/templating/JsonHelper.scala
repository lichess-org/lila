package lila.app
package templating

import play.api.libs.json._
import play.twirl.api.Html

import lila.api.Context

trait JsonHelper {

  def toJson[A: Writes](map: Map[Int, A]): Html = toJson {
    map mapKeys (_.toString)
  }

  def toJson[A: Writes](a: A): Html = lila.common.String.html.safeJson(Json toJson a)

  def htmlOrNull[A, B](a: Option[A])(f: A => Html) = a.fold(Html("null"))(f)

  def jsOrNull[A: Writes](a: Option[A]) = a.fold(Html("null"))(x => toJson(x))

  def jsUserId(implicit ctx: Context) = Html {
    ctx.userId.fold("null")(id => s""""$id"""")
  }
}
