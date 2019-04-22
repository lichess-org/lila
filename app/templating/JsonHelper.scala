package lidraughts.app
package templating

import play.api.libs.json._
import lidraughts.app.ui.ScalatagsTemplate._

import lidraughts.api.Context

trait JsonHelper {

  def toJsonString[A: Writes](a: A): String = lidraughts.common.String.html.safeJsonValue(Json toJson a)

  def jsOrNull[A: Writes](a: Option[A]): String = a.fold("null")(x => toJsonString(x))

  def jsUserIdString(implicit ctx: Context) = ctx.userId.fold("null")(id => s""""$id"""")
}
