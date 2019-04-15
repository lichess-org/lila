package lidraughts.app
package templating

import play.api.libs.json._
import play.twirl.api.Html
import lidraughts.app.ui.ScalatagsTemplate._

import lidraughts.api.Context

trait JsonHelper {

  def toJsonHtml[A: Writes](map: Map[Int, A]): Html = toJsonHtml {
    map mapKeys (_.toString)
  }

  def toJsonHtml[A: Writes](a: A): Html = lidraughts.common.String.html.safeJsonHtml(Json toJson a)
  def toJsonFrag[A: Writes](a: A): Frag = raw(lidraughts.common.String.html.safeJsonValue(Json toJson a))

  def htmlOrNull[A, B](a: Option[A])(f: A => Html) = a.fold(Html("null"))(f)

  def jsOrNull[A: Writes](a: Option[A]) = a.fold(Html("null"))(x => toJsonHtml(x))

  def jsUserIdString(implicit ctx: Context) = ctx.userId.fold("null")(id => s""""$id"""")
  def jsUserId(implicit ctx: Context) = jsUserIdString(ctx)
}
