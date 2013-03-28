package lila.app
package mod

import user.User

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

case class Modlog(
    mod: String,
    user: Option[String],
    action: String,
    details: Option[String] = None,
    date: DateTime = DateTime.now) {

  def showAction = action match {
    case Modlog.engine     ⇒ "mark as engine"
    case Modlog.unengine   ⇒ "un-mark as engine"
    case Modlog.deletePost ⇒ "delete forum post"
    case Modlog.ban        ⇒ "ban user"
    case Modlog.ipban      ⇒ "ban IP"
    case a                 ⇒ a
  }
}

object Modlog extends Function5[String, Option[String], String, Option[String], DateTime, Modlog] {

  val engine = "engine"
  val unengine = "unengine"
  val mute = "mute"
  val unmute = "unmute"
  val ban = "ban"
  val ipban = "ipban"
  val deletePost = "deletePost"

  import play.api.libs.json.Json

  val json = mongodb.Tube(Json.reads[Modlog], Json.writes[Modlog])
}
