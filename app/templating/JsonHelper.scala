package lila.app
package templating

import play.api.libs.json._
import lila.app.ui.ScalatagsTemplate._

import lila.api.Context

trait JsonHelper {

  def toJsonFrag[A: Writes](a: A): Frag = raw(toJsonString(a))
  def toJsonString[A: Writes](a: A): String = lila.common.String.html.safeJsonValue(Json toJson a)

  def jsOrNull[A: Writes](a: Option[A]): String = a.fold("null")(x => toJsonString(x))

  def jsUserIdString(implicit ctx: Context) = ctx.userId.fold("null")(id => s""""$id"""")
}
