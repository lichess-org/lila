package lila.mod

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime

case class Modlog(
    mod: String,
    user: Option[String],
    action: String,
    details: Option[String] = None,
    date: DateTime = DateTime.now) {

  def showAction = action match {
    case Modlog.engine        => "mark as engine"
    case Modlog.unengine      => "un-mark as engine"
    case Modlog.deletePost    => "delete forum post"
    case Modlog.ban           => "ban user"
    case Modlog.ipban         => "ban IPs"
    case Modlog.ipunban       => "unban IPs"
    case Modlog.closeAccount  => "close account"
    case Modlog.reopenAccount => "reopen account"
    case Modlog.openTopic     => "reopen topic"
    case Modlog.closeTopic    => "close topic"
    case Modlog.showTopic     => "show topic"
    case Modlog.hideTopic     => "hide topic"
    case Modlog.setTitle      => "set FIDE title"
    case Modlog.removeTitle   => "remove FIDE title"
    case a                    => a
  }

  override def toString = s"$mod $showAction $user"
}

object Modlog {

  val engine = "engine"
  val unengine = "unengine"
  val troll = "troll"
  val untroll = "untroll"
  val ban = "ban"
  val ipban = "ipban"
  val closeAccount = "closeAccount"
  val reopenAccount = "reopenAccount"
  val ipunban = "ipunban"
  val deletePost = "deletePost"
  val openTopic = "openTopic"
  val closeTopic = "closeTopic"
  val showTopic = "showTopic"
  val hideTopic = "hideTopic"
  val setTitle = "setTitle"
  val removeTitle = "removeTitle"

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
