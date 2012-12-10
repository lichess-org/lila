package lila
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

object Modlog {

  val engine = "engine"
  val unengine = "unengine"
  val mute = "mute"
  val unmute = "unmute"
  val ban = "ban"
  val ipban = "ipban"
  val deletePost = "deletePost"

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val json = mongodb.JsonTube((
    (__ \ 'mod).read[String] and
    (__ \ 'user).read[Option[String]] and
    (__ \ 'action).read[String] and
    (__ \ 'details).read[Option[String]] and
    (__ \ 'date).read[DateTime]
  )(Modlog.apply _),
    Json.writes[Modlog]
  )
}
