package lila
package mod

import user.User

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

case class Modlog(
  mod: String,
  user: Option[String],
  action: String,
  date: DateTime = DateTime.now)

object Modlog {

  val engine = "engine"
  val unengine = "un-engine"
  val mute = "mute"
  val unmute = "un-mute"
  val ipban = "ipban"
  val deletePost = "remove post"
}
