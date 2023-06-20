package lila.mod

import lila.report.{ ModId, Mod, Suspect }
import lila.user.Me

case class Modlog(
    mod: ModId,
    user: Option[UserId],
    action: String,
    details: Option[String] = None,
    date: Instant = nowInstant,
    index: Option[String] = None
):

  def isLichess = mod is lila.user.User.lichessId

  def notable      = action != Modlog.terminateTournament
  def notableZulip = notable && !isLichess

  def gameId = details.ifTrue(action == Modlog.cheatDetected).so(_.split(' ').lift(1))

  def indexAs(i: String) = copy(index = i.some)

  def showAction = action match
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
    case Modlog.showTopic           => "show topic"         // BC
    case Modlog.hideTopic           => "unfeature topic"    // BC
    case Modlog.stickyTopic         => "sticky topic"
    case Modlog.unstickyTopic       => "un-sticky topic"
    case Modlog.postAsAnonMod       => "post as a lichess moderator"
    case Modlog.editAsAnonMod       => "edit a lichess moderator post"
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
    case Modlog.prizeban            => "prizeban"
    case Modlog.unprizeban          => "un-prizeban"
    case Modlog.modMessage          => "send message"
    case Modlog.coachReview         => "disapprove coach review"
    case Modlog.cheatDetected       => "game lost by cheat detection"
    case Modlog.cli                 => "run CLI command"
    case Modlog.garbageCollect      => "garbage collect"
    case Modlog.streamerDecline     => "decline streamer"
    case Modlog.streamerList        => "list streamer"
    case Modlog.streamerUnlist      => "unlist streamer"
    case Modlog.streamerFeature     => "feature streamer"   // BC
    case Modlog.streamerUnfeature   => "unfeature streamer" // BC
    case Modlog.streamerTier        => "set streamer tier"
    case Modlog.blogTier            => "set blog tier"
    case Modlog.blogPostEdit        => "edit blog post"
    case Modlog.teamKick            => "kick from team"
    case Modlog.teamEdit            => "edited team"
    case Modlog.appealPost          => "posted in appeal"
    case Modlog.setKidMode          => "set kid mode"
    case Modlog.weakPassword        => "log in with weak password"
    case Modlog.blankedPassword     => "log in with blanked password"
    case a                          => a

  override def toString = s"$mod $showAction $user $details"

object Modlog:

  def apply(user: Option[UserId], action: String, details: Option[String])(using me: Me.Id): Modlog =
    Modlog(me.modId, user, action, details)

  def apply(user: Option[UserId], action: String)(using me: Me.Id): Modlog =
    Modlog(me.modId, user, action, none)

  def make(sus: Suspect, action: String, details: Option[String] = None)(using me: Me.Id): Modlog =
    Modlog(
      mod = me.modId,
      user = sus.user.id.some,
      action = action,
      details = details
    )

  case class UserEntry(user: UserId, action: String, date: Instant)

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
  val postAsAnonMod       = "postAsAnonMod"
  val editAsAnonMod       = "editAsAnonMod"
  val setTitle            = "setTitle"
  val removeTitle         = "removeTitle"
  val setEmail            = "setEmail"
  val practiceConfig      = "practiceConfig"
  val deleteTeam          = "deleteTeam"
  val disableTeam         = "disableTeam"
  val enableTeam          = "enableTeam"
  val terminateTournament = "terminateTournament"
  val chatTimeout         = "chatTimeout"
  val kickFromRankings    = "kickFromRankings"
  val reportban           = "reportban"
  val unreportban         = "unreportban"
  val rankban             = "rankban"
  val unrankban           = "unrankban"
  val prizeban            = "prizeban"
  val unprizeban          = "unprizeban"
  val modMessage          = "modMessage"
  val coachReview         = "coachReview"
  val cheatDetected       = "cheatDetected"
  val cli                 = "cli"
  val garbageCollect      = "garbageCollect"
  val streamerDecline     = "streamerDecline"
  val streamerList        = "streamerList"
  val streamerUnlist      = "streamerunlist"
  val streamerFeature     = "streamerFeature"   // BC
  val streamerUnfeature   = "streamerUnfeature" // BC
  val streamerTier        = "streamerTier"
  val blogTier            = "blogTier"
  val blogPostEdit        = "blogPostEdit"
  val teamKick            = "teamKick"
  val teamEdit            = "teamEdit"
  val appealPost          = "appealPost"
  val setKidMode          = "setKidMode"
  val weakPassword        = "weakPassword"
  val blankedPassword     = "blankedPassword"

  private val explainRegex = """^[\w-]{3,}+: (.++)$""".r
  def explain(e: Modlog) = (e.index has "team") so ~e.details match
    case explainRegex(explain) => explain.some
    case _                     => none
