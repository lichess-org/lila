package lila.mod

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

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[mod] lazy val tube = Tube[Modlog](
    (__.json update (
      merge(defaults) andThen readDate('date)
    )) andThen Json.reads[Modlog],
    Json.writes[Modlog] andThen (__.json update writeDate('date)),
    flags = Seq(_.NoId)
  )

  private def defaults = Json.obj("details" -> none[String])
}
