package lila
package mod

import user.User

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

case class Modlog(
    mod: String,
    user: Option[String],
    action: String,
    date: DateTime = DateTime.now) {

  def showAction = action match {
    case Modlog.engine     ⇒ "mark as engine"
    case Modlog.unengine   ⇒ "un-mark as engine"
    case Modlog.deletePost ⇒ "delete forum post"
    case a                 ⇒ a
  }
}

object Modlog {

  val engine = "engine"
  val unengine = "unengine"
  val mute = "mute"
  val unmute = "unmute"
  val ipban = "ipban"
  val deletePost = "deletePost"
}
