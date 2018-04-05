package lila.security

sealed abstract class Permission(val name: String, val children: List[Permission] = Nil) {

  final def is(p: Permission): Boolean = this == p || children.exists(_ is p)
}

object Permission {

  case object ViewBlurs extends Permission("ROLE_VIEW_BLURS")
  case object StaffForum extends Permission("ROLE_STAFF_FORUM")
  case object ModerateForum extends Permission("ROLE_MODERATE_FORUM")

  case object ModerateQa extends Permission("ROLE_MODERATE_QA")

  case object ChatTimeout extends Permission("ROLE_CHAT_TIMEOUT")
  case object UserSpy extends Permission("ROLE_USER_SPY")
  case object UserEvaluate extends Permission("ROLE_USER_EVALUATE")
  case object ViewPrivateComms extends Permission("ROLE_VIEW_PRIVATE_COMS")
  case object MarkTroll extends Permission("ROLE_CHAT_BAN", List(UserSpy, ChatTimeout))
  case object MarkEngine extends Permission("ROLE_ADJUST_CHEATER", List(UserSpy))
  case object MarkBooster extends Permission("ROLE_ADJUST_BOOSTER", List(UserSpy))
  case object IpBan extends Permission("ROLE_IP_BAN", List(UserSpy))
  case object CloseAccount extends Permission("ROLE_CLOSE_ACCOUNT", List(UserSpy))
  case object ReopenAccount extends Permission("ROLE_REOPEN_ACCOUNT", List(UserSpy))
  case object SetTitle extends Permission("ROLE_SET_TITLE", List(UserSpy))
  case object SetEmail extends Permission("ROLE_SET_EMAIL", List(UserSpy))
  case object SeeReport extends Permission("ROLE_SEE_REPORT")
  case object ModLog extends Permission("ROLE_MOD_LOG")
  case object SeeInsight extends Permission("ROLE_SEE_INSIGHT")
  case object StreamConfig extends Permission("ROLE_STREAM_CONFIG")
  case object PracticeConfig extends Permission("ROLE_PRACTICE_CONFIG")
  case object Beta extends Permission("ROLE_BETA")
  case object MessageAnyone extends Permission("ROLE_MESSAGE_ANYONE")
  case object UserSearch extends Permission("ROLE_USER_SEARCH")
  case object CloseTeam extends Permission("ROLE_CLOSE_TEAM")
  case object TerminateTournament extends Permission("ROLE_TERMINATE_TOURNAMENT")
  case object ManageTournament extends Permission("ROLE_MANAGE_TOURNAMENT")
  case object ManageEvent extends Permission("ROLE_MANAGE_EVENT")
  case object ChangePermission extends Permission("ROLE_CHANGE_PERMISSION")
  case object PublicMod extends Permission("ROLE_PUBLIC_MOD")
  case object Developer extends Permission("ROLE_DEVELOPER")
  case object Coach extends Permission("ROLE_COACH")
  case object ModNote extends Permission("ROLE_MOD_NOTE")
  case object RemoveRanking extends Permission("ROLE_REMOVE_RANKING")
  case object ReportBan extends Permission("ROLE_REPORT_BAN")
  case object ModMessage extends Permission("ROLE_MOD_MESSAGE")
  case object Impersonate extends Permission("ROLE_IMPERSONATE")
  case object DisapproveCoachReview extends Permission("ROLE_DISAPPROVE_COACH_REVIEW")
  case object PayPal extends Permission("ROLE_PAYPAL")
  case object Relay extends Permission("ROLE_RELAY")
  case object Cli extends Permission("ROLE_ClI")
  case object Settings extends Permission("ROLE_SETTINGS")
  case object Streamers extends Permission("ROLE_STREAMERS")

  case object Hunter extends Permission("ROLE_HUNTER", List(
    ViewBlurs, MarkEngine, MarkBooster, StaffForum,
    UserSpy, UserEvaluate, SeeReport, ModLog, SeeInsight,
    UserSearch, ModNote, RemoveRanking, ModMessage
  ))

  case object Admin extends Permission("ROLE_ADMIN", List(
    Hunter, ModerateForum, IpBan, CloseAccount, ReopenAccount, ViewPrivateComms,
    ChatTimeout, MarkTroll, SetTitle, SetEmail, ModerateQa, StreamConfig,
    MessageAnyone, CloseTeam, TerminateTournament, ManageTournament, ManageEvent,
    PracticeConfig, RemoveRanking, ReportBan, Beta, DisapproveCoachReview,
    Relay, Streamers
  ))

  case object SuperAdmin extends Permission("ROLE_SUPER_ADMIN", List(
    Admin, ChangePermission, Developer, Impersonate, PayPal, Cli, Settings
  ))

  lazy val allButSuperAdmin: List[Permission] = List(
    Admin, Hunter, MarkTroll, ChatTimeout, ChangePermission, ViewBlurs, StaffForum, ModerateForum,
    UserSpy, MarkEngine, MarkBooster, IpBan, ModerateQa, StreamConfig, PracticeConfig,
    Beta, MessageAnyone, UserSearch, CloseTeam, TerminateTournament, ManageTournament, ManageEvent,
    PublicMod, Developer, Coach, ModNote, RemoveRanking, ReportBan,
    Relay, Cli, Settings, Streamers
  )

  lazy private val all: List[Permission] = SuperAdmin :: allButSuperAdmin

  lazy private val allByName: Map[String, Permission] = all map { p => (p.name, p) } toMap

  def apply(name: String): Option[Permission] = allByName get name

  def apply(names: List[String]): List[Permission] = names flatMap { apply(_) }

  def exists(name: String) = allByName contains name
}
