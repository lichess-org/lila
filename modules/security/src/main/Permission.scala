package lila.security

sealed abstract class Permission(val key: String, val children: List[Permission] = Nil, val name: String):

  def this(key: String, name: String) = this(key, Nil, name)

  final def is(p: Permission): Boolean = this == p || children.exists(_ is p)

  val dbKey = s"ROLE_$key"

object Permission:

  type Selector = Permission.type => Permission

  case object ViewBlurs        extends Permission("VIEW_BLURS", "View blurs")
  case object ModerateForum    extends Permission("MODERATE_FORUM", "Moderate forum")
  case object ModerateBlog     extends Permission("MODERATE_BLOG", "Moderate blog")
  case object ChatTimeout      extends Permission("CHAT_TIMEOUT", "Chat timeout")
  case object BroadcastTimeout extends Permission("BROADCAST_TIMEOUT", "Broadcast timeout")
  case object PublicChatView   extends Permission("VIEW_PUBLIC_CHAT", "See public chat page")
  case object GamifyView       extends Permission("GAMIFY_VIEW", "See mod leaderboard")
  case object UserModView      extends Permission("USER_SPY", "User profile mod view")
  case object UserEvaluate     extends Permission("USER_EVALUATE", "Request evaluation")
  case object GamesModView     extends Permission("GAMES_MOD_VIEW", "User games mod view")
  case object SendToZulip      extends Permission("NOTIFY_SLACK", List(UserModView), "Send to Zulip")
  case object ViewPrivateComms extends Permission("VIEW_PRIVATE_COMS", "View private comms")
  case object Shadowban        extends Permission("SHADOWBAN", List(UserModView, ChatTimeout), "Shadowban")
  case object SetKidMode       extends Permission("SET_KID_MODE", List(UserModView), "Set Kid Mode")
  case object MarkEngine       extends Permission("ADJUST_CHEATER", List(UserModView), "Mark as cheater")
  case object MarkBooster      extends Permission("ADJUST_BOOSTER", List(UserModView), "Mark as booster")
  case object IpBan            extends Permission("IP_BAN", List(UserModView, ViewPrintNoIP), "IP ban")
  case object PrintBan         extends Permission("PRINT_BAN", List(UserModView), "Print ban")
  case object ViewPrintNoIP    extends Permission("VIEW_PRINT_NOIP", "View Print & NoIP")
  case object DisableTwoFactor extends Permission("DISABLE_2FA", "Disable 2FA")
  case object CloseAccount     extends Permission("CLOSE_ACCOUNT", List(UserModView), "Close/reopen account")
  case object GdprErase        extends Permission("GDPR_ERASE", List(CloseAccount), "GDPR erase account")
  case object SetTitle         extends Permission("SET_TITLE", List(UserModView), "Set/unset title")
  case object SetEmail         extends Permission("SET_EMAIL", "Set email address")
  case object SeeReport        extends Permission("SEE_REPORT", "See reports")
  case object Appeals          extends Permission("APPEAL", "Handle appeals")
  case object Presets          extends Permission("PRESET", "Edit mod presets")
  case object ModLog           extends Permission("MOD_LOG", "See mod log")
  case object SeeInsight       extends Permission("SEE_INSIGHT", "View player insights")
  case object PracticeConfig   extends Permission("PRACTICE_CONFIG", "Configure practice")
  case object PuzzleCurator    extends Permission("PUZZLE_CURATOR", "Classify puzzles")
  case object OpeningWiki      extends Permission("OPENING_WIKI", "Opening wiki")
  case object Beta             extends Permission("BETA", "Beta features")
  case object UserSearch       extends Permission("USER_SEARCH", "Mod user search")
  case object ManageTeam       extends Permission("MANAGE_TEAM", "Manage teams")
  case object ManageTournament extends Permission("MANAGE_TOURNAMENT", "Manage tournaments")
  case object ManageEvent      extends Permission("MANAGE_EVENT", "Manage events")
  case object ManageSimul      extends Permission("MANAGE_SIMUL", "Manage simuls")
  case object ChangePermission extends Permission("CHANGE_PERMISSION", "Change permissions")
  case object PublicMod        extends Permission("PUBLIC_MOD", "Mod badge")
  case object Developer        extends Permission("DEVELOPER", "Developer badge")
  case object ContentTeam      extends Permission("CONTENT_TEAM", "Content Team badge")
  case object Coach            extends Permission("COACH", "Is a coach")
  case object Teacher          extends Permission("TEACHER", "Is a class teacher")
  case object ModNote          extends Permission("MOD_NOTE", "Mod notes")
  case object RemoveRanking    extends Permission("REMOVE_RANKING", "Remove from ranking")
  case object ReportBan        extends Permission("REPORT_BAN", "Report ban")
  case object ArenaBan         extends Permission("ARENA_BAN", "Ban from arenas")
  case object PrizeBan         extends Permission("PRIZE_BAN", "Ban from prized tournaments")
  case object ModMessage       extends Permission("MOD_MESSAGE", "Send mod messages")
  case object Impersonate      extends Permission("IMPERSONATE", "Impersonate")
  case object DisapproveCoachReview extends Permission("DISAPPROVE_COACH_REVIEW", "Disapprove coach review")
  case object PayPal                extends Permission("PAYPAL", "PayPal")
  case object Relay                 extends Permission("RELAY", "Manage broadcasts")
  case object Cli                   extends Permission("CLI", "Command line")
  case object Settings              extends Permission("SETTINGS", "Lila settings")
  case object Streamers             extends Permission("STREAMERS", "Manage streamers")
  case object Verified              extends Permission("VERIFIED", "Verified badge")
  case object Prismic               extends Permission("PRISMIC", "Prismic preview")
  case object MonitoredCheatMod     extends Permission("MONITORED_MOD_CHEAT", "Monitored mod: cheat")
  case object MonitoredBoostMod     extends Permission("MONITORED_MOD_BOOST", "Monitored mod: boost")
  case object MonitoredCommMod      extends Permission("MONITORED_MOD_COMM", "Monitored mod: comms")
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

  case object BoostHunter
      extends Permission(
        "BOOST_HUNTER",
        List(
          LichessTeam,
          MarkBooster,
          ArenaBan,
          UserModView,
          GamesModView,
          GamifyView,
          SeeReport,
          ModLog,
          ModMessage,
          ModNote,
          ViewPrintNoIP,
          SendToZulip
        ),
        "Boost Hunter"
      )

  case object CheatHunter
      extends Permission(
        "CHEAT_HUNTER",
        List(
          LichessTeam,
          ViewBlurs,
          MarkEngine,
          UserModView,
          GamesModView,
          GamifyView,
          UserEvaluate,
          SeeReport,
          ModLog,
          SeeInsight,
          UserSearch,
          ModMessage,
          ModNote,
          ViewPrintNoIP,
          SendToZulip
        ),
        "Cheat Hunter"
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
          ModerateBlog,
          ReportBan,
          ModMessage,
          SeeReport,
          ModLog,
          ModNote,
          ViewPrintNoIP,
          SendToZulip
        ),
        "Shusher"
      )

  case object EmailAnswerer
      extends Permission(
        "EMAIL_ANSWERER",
        List(
          LichessTeam,
          UserSearch,
          CloseAccount,
          GdprErase,
          SetEmail,
          DisableTwoFactor
        ),
        "Email answerer"
      )

  case object Admin
      extends Permission(
        "ADMIN",
        List(
          LichessTeam,
          PrizeBan,
          RemoveRanking,
          BoostHunter,
          CheatHunter,
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
          PracticeConfig,
          PuzzleCurator,
          OpeningWiki,
          Presets,
          Relay,
          Streamers,
          DisableTwoFactor,
          ChangePermission,
          StudyAdmin,
          BroadcastTimeout,
          ApiChallengeAdmin
        ),
        "Admin"
      )

  case object SuperAdmin
      extends Permission(
        "SUPER_ADMIN",
        List(
          Admin,
          GdprErase,
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
      ModerateBlog,
      ReportBan,
      ModMessage
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
      GdprErase,
      SetTitle,
      SetEmail
    ),
    "Misc mod" -> List(
      SeeReport,
      GamifyView,
      Appeals,
      UserSearch,
      ModNote,
      ModLog,
      ManageTeam,
      Streamers
    ),
    "Monitoring" -> List(
      MonitoredBoostMod,
      MonitoredCheatMod,
      MonitoredCommMod
    ),
    "Content" -> List(
      Relay,
      BroadcastTimeout,
      ManageEvent,
      ManageTournament,
      ManageSimul,
      StudyAdmin,
      PracticeConfig,
      PuzzleCurator,
      OpeningWiki,
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
      Verified,
      ContentTeam
    ),
    "Package" -> List(
      LichessTeam,
      TimeoutMod,
      BoostHunter,
      CheatHunter,
      Shusher,
      EmailAnswerer,
      Admin,
      SuperAdmin
    )
  )

  lazy val all: Set[Permission] = categorized.flatMap { (_, perms) => perms }.toSet

  lazy val nonModPermissions: Set[Permission] =
    Set(Beta, Prismic, Coach, Teacher, Developer, Verified, ContentTeam, ApiHog, Relay)

  lazy val modPermissions: Set[Permission] = all diff nonModPermissions

  lazy val allByDbKey: Map[String, Permission] = all.mapBy(_.dbKey)

  def apply(dbKey: String): Option[Permission] = allByDbKey get dbKey

  def apply(dbKeys: Seq[String]): Set[Permission] = dbKeys flatMap allByDbKey.get toSet

  def expanded(dbKeys: Seq[String]): Set[Permission] =
    val level0 = apply(dbKeys)
    val level1 = level0.flatMap(_.children)
    val level2 = level1.flatMap(_.children)
    level0 ++ level1 ++ level2

  def findGranterPackage(perms: Set[Permission], perm: Permission): Option[Permission] =
    !perms(perm) so perms.find(_ is perm)

  def diff(orig: Set[Permission], dest: Set[Permission]): Map[Permission, Boolean] = {
    orig.diff(dest).map(_ -> false) ++ dest.diff(orig).map(_ -> true)
  }.toMap
