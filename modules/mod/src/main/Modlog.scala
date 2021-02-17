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

  def isLichess = mod == lila.user.User.lichessId

  def notable      = action != Modlog.terminateTournament
  def notableSlack = notable && !isLichess

  def gameId = details.ifTrue(action == Modlog.cheatDetected).??(_.split(' ').lift(1))

  def showAction =
    action match {
      case Modlog.alt                 => "mark as alt"
      case Modlog.unalt               => "un-mark as alt"
      case Modlog.engine              => "mark as engine"
      case Modlog.unengine            => "un-mark as engine"
      case Modlog.booster             => "mark as booster"
      case Modlog.unbooster           => "un-mark as booster"
      case Modlog.deletePost          => "delete forum post"
      case Modlog.disableTwoFactor    => "disable 2fa"
      case Modlog.closeAccount        => "close account"
      case Modlog.selfCloseAccount    => "self close account"
      case Modlog.reopenAccount       => "reopen account"
      case Modlog.openTopic           => "reopen topic"
      case Modlog.closeTopic          => "close topic"
      case Modlog.showTopic           => "show topic"
      case Modlog.hideTopic           => "unfeature topic"
      case Modlog.stickyTopic         => "sticky topic"
      case Modlog.unstickyTopic       => "un-sticky topic"
      case Modlog.setTitle            => "set FIDE title"
      case Modlog.removeTitle         => "remove FIDE title"
      case Modlog.setEmail            => "set email address"
      case Modlog.practiceConfig      => "update practice config"
      case Modlog.deleteTeam          => "delete team"
      case Modlog.disableTeam         => "disable team"
      case Modlog.enableTeam          => "enable team"
      case Modlog.terminateTournament => "terminate tournament"
      case Modlog.chatTimeout         => "chat timeout"
      case Modlog.troll               => "shadowban"
      case Modlog.untroll             => "un-shadowban"
      case Modlog.permissions         => "set permissions"
      case Modlog.kickFromRankings    => "kick from rankings"
      case Modlog.reportban           => "reportban"
      case Modlog.unreportban         => "un-reportban"
      case Modlog.rankban             => "rankban"
      case Modlog.unrankban           => "un-rankban"
      case Modlog.modMessage          => "send message"
      case Modlog.coachReview         => "disapprove coach review"
      case Modlog.cheatDetected       => "game lost by cheat detection"
      case Modlog.cli                 => "run CLI command"
      case Modlog.garbageCollect      => "garbage collect"
      case Modlog.streamerList        => "list streamer"
      case Modlog.streamerUnlist      => "unlist streamer"
      case Modlog.streamerFeature     => "feature streamer"   // BC
      case Modlog.streamerUnfeature   => "unfeature streamer" // BC
      case Modlog.streamerTier        => "set streamer tier"
      case Modlog.teamKick            => "kick from team"
      case Modlog.teamEdit            => "edited team"
      case Modlog.appealPost          => "posted in appeal"
      case Modlog.setKidMode          => "set kid mode"
      case a                          => a
    }

  override def toString = s"$mod $showAction ${~user} $details"
}

object Modlog {

  def make(mod: Mod, sus: Suspect, action: String, details: Option[String] = None): Modlog =
    Modlog(
      mod = mod.user.id,
      user = sus.user.id.some,
      action = action,
      details = details
    )

  val alt                 = "alt"
  val unalt               = "unalt"
  val engine              = "engine"
  val unengine            = "unengine"
  val booster             = "booster"
  val unbooster           = "unbooster"
  val troll               = "troll"
  val untroll             = "untroll"
  val permissions         = "permissions"
  val disableTwoFactor    = "disableTwoFactor"
  val closeAccount        = "closeAccount"
  val selfCloseAccount    = "selfCloseAccount"
  val reopenAccount       = "reopenAccount"
  val deletePost          = "deletePost"
  val openTopic           = "openTopic"
  val closeTopic          = "closeTopic"
  val showTopic           = "showTopic"
  val hideTopic           = "hideTopic"
  val stickyTopic         = "stickyTopic"
  val unstickyTopic       = "unstickyTopic"
  val setTitle            = "setTitle"
  val removeTitle         = "removeTitle"
  val setEmail            = "setEmail"
  val practiceConfig      = "practiceConfig"
  val deleteTeam          = "deleteTeam"
  val disableTeam         = "disableTeam"
  val enableTeam          = "enableTeam"
  val terminateTournament = "terminateTournament "
  val chatTimeout         = "chatTimeout "
  val kickFromRankings    = "kickFromRankings"
  val reportban           = "reportban"
  val unreportban         = "unreportban"
  val rankban             = "rankban"
  val unrankban           = "unrankban"
  val modMessage          = "modMessage"
  val coachReview         = "coachReview"
  val cheatDetected       = "cheatDetected"
  val cli                 = "cli"
  val garbageCollect      = "garbageCollect"
  val streamerList        = "streamerList"
  val streamerUnlist      = "streamerunlist"
  val streamerFeature     = "streamerFeature"   // BC
  val streamerUnfeature   = "streamerUnfeature" // BC
  val streamerTier        = "streamerTier"
  val teamKick            = "teamKick"
  val teamEdit            = "teamEdit"
  val appealPost          = "appealPost"
  val setKidMode          = "setKidMode"
}
