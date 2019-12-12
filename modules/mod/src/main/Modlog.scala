package lila.mod

import org.joda.time.DateTime

import lila.report.{ Mod, Suspect }

case class Modlog(
    mod: String,
    user: Option[String],
    action: String,
    details: Option[String] = None,
    date: DateTime = DateTime.now
) {

  def showAction = action match {
    case Modlog.engine => "mark as engine"
    case Modlog.unengine => "un-mark as engine"
    case Modlog.booster => "mark as booster"
    case Modlog.unbooster => "un-mark as booster"
    case Modlog.deletePost => "delete forum post"
    case Modlog.ban => "ban user"
    case Modlog.ipban => "ban IPs"
    case Modlog.ipunban => "unban IPs"
    case Modlog.disableTwoFactor => "disable 2fa"
    case Modlog.closeAccount => "close account"
    case Modlog.selfCloseAccount => "self close account"
    case Modlog.reopenAccount => "reopen account"
    case Modlog.openTopic => "reopen topic"
    case Modlog.closeTopic => "close topic"
    case Modlog.showTopic => "show topic"
    case Modlog.hideTopic => "unfeature topic"
    case Modlog.stickyTopic => "sticky topic"
    case Modlog.unstickyTopic => "un-sticky topic"
    case Modlog.setTitle => "set FIDE title"
    case Modlog.removeTitle => "remove FIDE title"
    case Modlog.setEmail => "set email address"
    case Modlog.practiceConfig => "update practice config"
    case Modlog.deleteTeam => "delete team"
    case Modlog.terminateTournament => "terminate tournament"
    case Modlog.chatTimeout => "chat timeout"
    case Modlog.troll => "shadowban"
    case Modlog.untroll => "un-shadowban"
    case Modlog.permissions => "set permissions"
    case Modlog.kickFromRankings => "kick from rankings"
    case Modlog.reportban => "reportban"
    case Modlog.unreportban => "un-reportban"
    case Modlog.rankban => "rankban"
    case Modlog.unrankban => "un-rankban"
    case Modlog.modMessage => "send message"
    case Modlog.coachReview => "disapprove coach review"
    case Modlog.cheatDetected => "game lost by cheat detection"
    case Modlog.cli => "run CLI command"
    case Modlog.garbageCollect => "garbage collect"
    case Modlog.streamerList => "list streamer"
    case Modlog.streamerUnlist => "unlist streamer"
    case Modlog.streamerFeature => "feature streamer"
    case Modlog.streamerUnfeature => "unfeature streamer"
    case Modlog.teamKick => "kick from team"
    case Modlog.teamEdit => "edited team"
    case Modlog.teamMadeOwner => "made team owner"
    case a => a
  }

  override def toString = s"$mod $showAction ${~user}"
}

object Modlog {

  def make(mod: Mod, sus: Suspect, action: String, details: Option[String] = None): Modlog =
    Modlog(
      mod = mod.user.id,
      user = sus.user.id.some,
      action = action,
      details = details
    )

  val engine = "engine"
  val unengine = "unengine"
  val booster = "booster"
  val unbooster = "unbooster"
  val troll = "troll"
  val untroll = "untroll"
  val permissions = "permissions"
  val ban = "ban"
  val ipban = "ipban"
  val disableTwoFactor = "disableTwoFactor"
  val closeAccount = "closeAccount"
  val selfCloseAccount = "selfCloseAccount"
  val reopenAccount = "reopenAccount"
  val ipunban = "ipunban"
  val deletePost = "deletePost"
  val openTopic = "openTopic"
  val closeTopic = "closeTopic"
  val showTopic = "showTopic"
  val hideTopic = "hideTopic"
  val stickyTopic = "stickyTopic"
  val unstickyTopic = "unstickyTopic"
  val setTitle = "setTitle"
  val removeTitle = "removeTitle"
  val setEmail = "setEmail"
  val practiceConfig = "practiceConfig"
  val deleteTeam = "deleteTeam"
  val terminateTournament = "terminateTournament "
  val chatTimeout = "chatTimeout "
  val kickFromRankings = "kickFromRankings"
  val reportban = "reportban"
  val unreportban = "unreportban"
  val rankban = "rankban"
  val unrankban = "unrankban"
  val modMessage = "modMessage"
  val coachReview = "coachReview"
  val cheatDetected = "cheatDetected"
  val cli = "cli"
  val garbageCollect = "garbageCollect"
  val streamerList = "streamerList"
  val streamerUnlist = "streamerunlist"
  val streamerFeature = "streamerFeature"
  val streamerUnfeature = "streamerUnfeature"
  val teamKick = "teamKick"
  val teamEdit = "teamEdit"
  val teamMadeOwner = "teamMadeOwner"
}
