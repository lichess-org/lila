package lila.mod

import org.joda.time.DateTime

case class Modlog(
    mod: String,
    user: Option[String],
    action: String,
    details: Option[String] = None,
    date: DateTime = DateTime.now) {

  def showAction = action match {
    case Modlog.engine              => "mark as engine"
    case Modlog.unengine            => "un-mark as engine"
    case Modlog.booster             => "mark as booster"
    case Modlog.unbooster           => "un-mark as booster"
    case Modlog.deletePost          => "delete forum post"
    case Modlog.ban                 => "ban user"
    case Modlog.ipban               => "ban IPs"
    case Modlog.ipunban             => "unban IPs"
    case Modlog.closeAccount        => "close account"
    case Modlog.reopenAccount       => "reopen account"
    case Modlog.openTopic           => "reopen topic"
    case Modlog.closeTopic          => "close topic"
    case Modlog.showTopic           => "show topic"
    case Modlog.hideTopic           => "unfeature topic"
    case Modlog.setTitle            => "set FIDE title"
    case Modlog.removeTitle         => "remove FIDE title"
    case Modlog.setEmail            => "set email address"
    case Modlog.deleteQaQuestion    => "delete Q&A question"
    case Modlog.deleteQaAnswer      => "delete Q&A answer"
    case Modlog.deleteQaComment     => "delete Q&A comment"
    case Modlog.streamConfig        => "update streams config"
    case Modlog.deleteTeam          => "delete team"
    case Modlog.terminateTournament => "terminate tournament"
    case Modlog.chatTimeout         => "chat timeout"
    case Modlog.troll               => "shadowban"
    case Modlog.untroll             => "un-shadowban"
    case a                          => a
  }

  override def toString = s"$mod $showAction ${~user}"
}

object Modlog {

  val engine = "engine"
  val unengine = "unengine"
  val booster = "booster"
  val unbooster = "unbooster"
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
  val setEmail = "setEmail"
  val deleteQaQuestion = "deleteQaQuestion"
  val deleteQaAnswer = "deleteQaAnswer"
  val deleteQaComment = "deleteQaComment"
  val streamConfig = "streamConfig"
  val deleteTeam = "deleteTeam"
  val terminateTournament = "terminateTournament "
  val chatTimeout = "chatTimeout "
}
