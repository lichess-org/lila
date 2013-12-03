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
    case Modlog.engine        ⇒ "mark as engine"
    case Modlog.unengine      ⇒ "un-mark as engine"
    case Modlog.deletePost    ⇒ "delete forum post"
    case Modlog.ban           ⇒ "ban user"
    case Modlog.ipban         ⇒ "ban IPs"
    case Modlog.ipunban       ⇒ "unban IPs"
    case Modlog.reopenAccount ⇒ "reopen account"
    case Modlog.openTopic     ⇒ "reopen topic"
    case Modlog.closeTopic    ⇒ "close topic"
    case a                    ⇒ a
  }
}

object Modlog {

  val engine = "engine"
  val unengine = "unengine"
  val troll = "troll"
  val untroll = "untroll"
  val ban = "ban"
  val ipban = "ipban"
  val reopenAccount = "reopenAccount"
  val ipunban = "ipunban"
  val deletePost = "deletePost"
  val closeTopic = "closeTopic"
  val openTopic = "openTopic"

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private[mod] lazy val tube = JsTube[Modlog](
    (__.json update (
      merge(defaults) andThen readDate('date)
    )) andThen Json.reads[Modlog],
    Json.writes[Modlog] andThen (__.json update writeDate('date)),
    flags = Seq(_.NoId)
  )

  private def defaults = Json.obj("details" -> none[String])
}
