package lila.security

sealed abstract class Permission(val key: String, val children: List[Permission] = Nil, val name: String) {

  def this(key: String, name: String) = this(key, Nil, name)

  final def is(p: Permission): Boolean = this == p || children.exists(_ is p)

  val dbKey = s"ROLE_$key"
}

object Permission {

  type Selector = Permission.type => Permission

  case object ViewBlurs     extends Permission("VIEW_BLURS", "View blurs")
  case object ModerateForum extends Permission("MODERATE_FORUM", "Moderate forum")

  case object ChatTimeout           extends Permission("CHAT_TIMEOUT", "Chat timeout")
  case object PublicChatView        extends Permission("VIEW_PUBLIC_CHAT", "See public chat page")
  case object GamifyView            extends Permission("GAMIFY_VIEW", "See mod leaderboard")
  case object UserModView           extends Permission("USER_SPY", "User profile mod view")
  case object UserEvaluate          extends Permission("USER_EVALUATE", "Request evaluation")
  case object NotifySlack           extends Permission("NOTIFY_SLACK", List(UserModView), "Notify #tavern")
  case object ViewPrivateComms      extends Permission("VIEW_PRIVATE_COMS", "View private comms")
  case object Shadowban             extends Permission("SHADOWBAN", List(UserModView, ChatTimeout), "Shadowban")
  case object SetKidMode            extends Permission("SET_KID_MODE", List(UserModView), "Set Kid Mode")
  case object MarkEngine            extends Permission("ADJUST_CHEATER", List(UserModView), "Mark as cheater")
  case object MarkBooster           extends Permission("ADJUST_BOOSTER", List(UserModView), "Mark as booster")
  case object IpBan                 extends Permission("IP_BAN", List(UserModView, ViewPrintNoIP), "IP ban")
  case object PrintBan              extends Permission("PRINT_BAN", List(UserModView), "Print ban")
  case object ViewPrintNoIP         extends Permission("VIEW_PRINT_NOIP", "View Print & NoIP")
  case object DisableTwoFactor      extends Permission("DISABLE_2FA", "Disable 2FA")
  case object CloseAccount          extends Permission("CLOSE_ACCOUNT", List(UserModView), "Close/reopen account")
  case object SetTitle              extends Permission("SET_TITLE", List(UserModView), "Set/unset title")
  case object SetEmail              extends Permission("SET_EMAIL", List(UserModView), "Set email address")
  case object SeeReport             extends Permission("SEE_REPORT", "See reports")
  case object Appeals               extends Permission("APPEAL", "Handle appeals")
  case object Presets               extends Permission("PRESET", "Edit mod presets")
  case object ModLog                extends Permission("MOD_LOG", "See mod log")
  case object SeeInsight            extends Permission("SEE_INSIGHT", "View player insights")
  case object PracticeConfig        extends Permission("PRACTICE_CONFIG", "Configure practice")
  case object PuzzleCurator         extends Permission("PUZZLE_CURATOR", "Classify puzzles")
  case object Beta                  extends Permission("BETA", "Beta features")
  case object UserSearch            extends Permission("USER_SEARCH", "Mod user search")
  case object ManageTeam            extends Permission("MANAGE_TEAM", "Manage teams")
  case object ManageTournament      extends Permission("MANAGE_TOURNAMENT", "Manage tournaments")
  case object ManageEvent           extends Permission("MANAGE_EVENT", "Manage events")
  case object ManageSimul           extends Permission("MANAGE_SIMUL", "Manage simuls")
  case object ChangePermission      extends Permission("CHANGE_PERMISSION", "Change permissions")
  case object PublicMod             extends Permission("PUBLIC_MOD", "Mod badge")
  case object Developer             extends Permission("DEVELOPER", "Developer badge")
  case object Coach                 extends Permission("COACH", "Is a coach")
  case object Teacher               extends Permission("TEACHER", "Is a class teacher")
  case object ModNote               extends Permission("MOD_NOTE", "Mod notes")
  case object RemoveRanking         extends Permission("REMOVE_RANKING", "Remove from ranking")
  case object ReportBan             extends Permission("REPORT_BAN", "Report ban")
  case object ModMessage            extends Permission("MOD_MESSAGE", "Send mod messages")
  case object Impersonate           extends Permission("IMPERSONATE", "Impersonate")
  case object DisapproveCoachReview extends Permission("DISAPPROVE_COACH_REVIEW", "Disapprove coach review")
  case object PayPal                extends Permission("PAYPAL", "PayPal")
  case object Relay                 extends Permission("RELAY", "Manage broadcasts")
  case object Cli                   extends Permission("ClI", "Command line")
  case object Settings              extends Permission("SETTINGS", "Lila settings")
  case object Streamers             extends Permission("STREAMERS", "Manage streamers")
  case object Verified              extends Permission("VERIFIED", "Verified badge")
  case object Prismic               extends Permission("PRISMIC", "Prismic preview")
  case object MonitoredMod          extends Permission("MONITORED_MOD", "Monitored mod")
  case object StudyAdmin            extends Permission("STUDY_ADMIN", "Study admin")
  case object ApiHog                extends Permission("API_HOG", "API hog")
  case object ApiChallengeAdmin     extends Permission("API_CHALLENGE_ADMIN", "API Challenge admin")

  case object LichessTeam
      extends Permission(
        "LICHESS_TEAM",
        List(Prismic),
        "Lichess team"
      )

  case object TimeoutMod
      extends Permission(
        "TIMEOUT_MOD",
        List(
          ChatTimeout,
          PublicChatView,
          GamifyView
        ),
        "Timeout mod"
      )

  case object Hunter
      extends Permission(
        "HUNTER",
        List(
          LichessTeam,
          ViewBlurs,
          MarkEngine,
          MarkBooster,
          CloseAccount,
          UserModView,
          GamifyView,
          UserEvaluate,
          SeeReport,
          ModLog,
          SeeInsight,
          UserSearch,
          RemoveRanking,
          ModMessage,
          ModNote,
          ViewPrintNoIP
        ),
        "Hunter"
      )

  case object Shusher
      extends Permission(
        "SHUSHER",
        List(
          LichessTeam,
          TimeoutMod,
          ViewPrivateComms,
          Shadowban,
          SetKidMode,
          ModerateForum,
          ReportBan,
          ModMessage,
          SeeReport,
          ModLog,
          ModNote,
          ViewPrintNoIP
        ),
        "Shusher"
      )

  case object Admin
      extends Permission(
        "ADMIN",
        List(
          Hunter,
          Shusher,
          Appeals,
          IpBan,
          PrintBan,
          CloseAccount,
          SetTitle,
          SetEmail,
          ManageTeam,
          ManageTournament,
          ManageSimul,
          ManageEvent,
          NotifySlack,
          PracticeConfig,
          PuzzleCurator,
          Presets,
          RemoveRanking,
          DisapproveCoachReview,
          Relay,
          Streamers,
          DisableTwoFactor,
          ChangePermission,
          StudyAdmin
        ),
        "Admin"
      )

  case object SuperAdmin
      extends Permission(
        "SUPER_ADMIN",
        List(
          Admin,
          Impersonate,
          PayPal,
          Cli,
          Settings
        ),
        "Super Admin"
      )

  lazy val categorized: List[(String, List[Permission])] = List(
    "Comm mod" -> List(
      ViewPrivateComms,
      Shadowban,
      SetKidMode,
      ChatTimeout,
      PublicChatView,
      ModerateForum,
      ReportBan,
      ModMessage,
      DisapproveCoachReview
    ),
    "Play mod" -> List(
      SeeInsight,
      ViewBlurs,
      MarkEngine,
      UserEvaluate,
      MarkBooster,
      RemoveRanking
    ),
    "Account mod" -> List(
      UserModView,
      IpBan,
      PrintBan,
      DisableTwoFactor,
      CloseAccount,
      SetTitle,
      SetEmail
    ),
    "Misc mod" -> List(
      SeeReport,
      GamifyView,
      Appeals,
      UserSearch,
      MonitoredMod,
      ModNote,
      ModLog,
      ManageTeam,
      Streamers
    ),
    "Content" -> List(
      Relay,
      ManageEvent,
      ManageTournament,
      ManageSimul,
      StudyAdmin,
      PracticeConfig,
      PuzzleCurator,
      Presets
    ),
    "Dev" -> List(
      Cli,
      Settings,
      Impersonate,
      ChangePermission,
      PayPal
    ),
    "Feature" -> List(
      Beta,
      Prismic,
      Coach,
      Teacher,
      ApiHog,
      ApiChallengeAdmin
    ),
    "Badge" -> List(
      Developer,
      PublicMod,
      Verified
    ),
    "Package" -> List(
      LichessTeam,
      TimeoutMod,
      Hunter,
      Shusher,
      Admin,
      SuperAdmin
    )
  )

  lazy val all: Set[Permission] = categorized.flatMap { case (_, perms) =>
    perms
  }.toSet

  lazy val nonModPermissions: Set[Permission] =
    Set(Beta, Prismic, Coach, Teacher, Developer, Verified, ApiHog, ApiChallengeAdmin)

  lazy val modPermissions: Set[Permission] = all diff nonModPermissions

  lazy val allByDbKey: Map[String, Permission] = all.view map { p =>
    (p.dbKey, p)
  } toMap

  def apply(dbKey: String): Option[Permission] = allByDbKey get dbKey

  def apply(dbKeys: Seq[String]): Set[Permission] = dbKeys flatMap allByDbKey.get toSet

  def findGranterPackage(perms: Set[Permission], perm: Permission): Option[Permission] =
    !perms(perm) ?? perms.find(_ is perm)

  def diff(orig: Set[Permission], dest: Set[Permission]): Map[Permission, Boolean] = {
    orig.diff(dest).map(_ -> false) ++ dest.diff(orig).map(_ -> true)
  }.toMap
}
