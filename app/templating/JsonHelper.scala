package lila.app
package templating

import play.api.libs.json._
import play.twirl.api.Html
import scalatags.Text.Frag

import lila.api.Context

trait JsonHelper {

  def toJsonHtml[A: Writes](map: Map[Int, A]): Html = toJsonHtml {
    map mapKeys (_.toString)
  }

  def toJsonHtml[A: Writes](a: A): Html = lila.common.String.html.safeJsonHtml(Json toJson a)
  def toJsonFrag[A: Writes](a: A): Frag = lila.common.String.html.safeJsonFrag(Json toJson a)

  def htmlOrNull[A, B](a: Option[A])(f: A => Html) = a.fold(Html("null"))(f)

  def jsOrNull[A: Writes](a: Option[A]) = a.fold(Html("null"))(x => toJsonHtml(x))

  def jsUserId(implicit ctx: Context) = Html {
    ctx.userId.fold("null")(id => s""""$id"""")
  }
}
