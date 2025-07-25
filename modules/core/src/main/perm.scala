package lila.core
package perm

import lila.core.user.{ Me, User, RoleDbKey }

object Granter:

  def apply(permission: Permission)(using me: Me): Boolean =
    me.enabled.yes && ofDbKeys(permission, me.roles)

  def apply(f: Permission.Selector)(using me: Me): Boolean =
    me.enabled.yes && ofDbKeys(f(Permission), me.roles)

  def opt(f: Permission.Selector)(using me: Option[Me]): Boolean =
    me.exists(of(f))

  def of(permission: Permission)(user: User): Boolean =
    user.enabled.yes && ofDbKeys(permission, user.roles)

  def of(f: Permission.Selector)(user: User): Boolean =
    user.enabled.yes && ofDbKeys(f(Permission), user.roles)

  def ofUser(f: Permission.Selector)(user: User): Boolean = of(f)(user)

  def ofDbKeys(permission: Permission, dbKeys: Seq[RoleDbKey]): Boolean =
    Permission.ofDbKeys(dbKeys).exists(_.grants(permission))
  def ofDbKeys(f: Permission.Selector, dbKeys: Seq[RoleDbKey]): Boolean =
    ofDbKeys(f(Permission), dbKeys)

enum Permission(val key: String, val alsoGrants: List[Permission], val name: String):
  def this(key: String, name: String) = this(key, Nil, name)
  def dbKey = RoleDbKey(s"ROLE_$key")
  final def grants(p: Permission): Boolean = this == p || alsoGrants.exists(_.grants(p))

  case ViewBlurs extends Permission("VIEW_BLURS", "View blurs")
  case ModerateForum extends Permission("MODERATE_FORUM", "Moderate forum")
  case ModerateBlog extends Permission("MODERATE_BLOG", "Moderate blog")
  case ChatTimeout extends Permission("CHAT_TIMEOUT", "Chat timeout")
  case BroadcastTimeout extends Permission("BROADCAST_TIMEOUT", "Broadcast timeout")
  case PublicChatView extends Permission("VIEW_PUBLIC_CHAT", "See public chat page")
  case GamifyView extends Permission("GAMIFY_VIEW", "See mod leaderboard")
  case UserModView extends Permission("USER_SPY", "User profile mod view")
  case UserEvaluate extends Permission("USER_EVALUATE", "Request evaluation")
  case GamesModView extends Permission("GAMES_MOD_VIEW", "User games mod view")
  case SendToZulip extends Permission("NOTIFY_SLACK", List(UserModView), "Send to Zulip")
  case ViewPrivateComms extends Permission("VIEW_PRIVATE_COMS", "View private comms")
  case Shadowban extends Permission("SHADOWBAN", List(UserModView, ChatTimeout), "Shadowban")
  case FullCommsExport extends Permission("FULL_COMMS_EXPORT", List(ViewPrivateComms), "Full comms export")
  case SetKidMode extends Permission("SET_KID_MODE", List(UserModView), "Set Kid Mode")
  case MarkEngine extends Permission("ADJUST_CHEATER", List(UserModView), "Mark as cheater")
  case MarkBooster extends Permission("ADJUST_BOOSTER", List(UserModView), "Mark as booster")
  case ViewPrintNoIP extends Permission("VIEW_PRINT_NOIP", "View Print & NoIP")
  case IpBan extends Permission("IP_BAN", List(UserModView, ViewPrintNoIP), "IP ban")
  case PrintBan extends Permission("PRINT_BAN", List(UserModView), "Print ban")
  case DisableTwoFactor extends Permission("DISABLE_2FA", "Disable 2FA")
  case CloseAccount extends Permission("CLOSE_ACCOUNT", List(UserModView), "Close/reopen account")
  case GdprErase extends Permission("GDPR_ERASE", List(CloseAccount), "GDPR erase account")
  case SetTitle extends Permission("SET_TITLE", List(UserModView), "Set/unset title")
  case TitleRequest extends Permission("TITLE_REQUEST", List(UserModView, SetTitle), "Process title requests")
  case SetEmail extends Permission("SET_EMAIL", "Set email address")
  case SeeReport extends Permission("SEE_REPORT", "See reports")
  case Appeals extends Permission("APPEAL", "Handle appeals")
  case Presets extends Permission("PRESET", "Edit mod presets")
  case ModLog extends Permission("MOD_LOG", "See mod log")
  case SeeInsight extends Permission("SEE_INSIGHT", "View player insights")
  case FreePatron extends Permission("FREE_PATRON", List(UserModView), "Give free patron")
  case PracticeConfig extends Permission("PRACTICE_CONFIG", "Configure practice")
  case PuzzleCurator extends Permission("PUZZLE_CURATOR", "Classify puzzles")
  case OpeningWiki extends Permission("OPENING_WIKI", "Opening wiki")
  case Beta extends Permission("BETA", "Beta features")
  case UserSearch extends Permission("USER_SEARCH", "Mod user search")
  case ManageTeam extends Permission("MANAGE_TEAM", "Manage teams")
  case ManageTournament extends Permission("MANAGE_TOURNAMENT", "Manage tournaments")
  case ManageEvent extends Permission("MANAGE_EVENT", "Manage events")
  case ManageSimul extends Permission("MANAGE_SIMUL", "Manage simuls")
  case ChangePermission extends Permission("CHANGE_PERMISSION", "Change permissions")
  case PublicMod extends Permission("PUBLIC_MOD", "Mod badge")
  case Developer extends Permission("DEVELOPER", "Developer badge")
  case ContentTeam extends Permission("CONTENT_TEAM", "Content Team badge")
  case BroadcastTeam extends Permission("BROADCAST_TEAM", "Broadcast Team badge")
  case Coach extends Permission("COACH", "Is a coach")
  case Teacher extends Permission("TEACHER", "Is a class teacher")
  case ModNote extends Permission("MOD_NOTE", "Mod notes")
  case RemoveRanking extends Permission("REMOVE_RANKING", "Remove from ranking")
  case ReportBan extends Permission("REPORT_BAN", "Report ban")
  case ArenaBan extends Permission("ARENA_BAN", "Ban from arenas")
  case PrizeBan extends Permission("PRIZE_BAN", "Ban from prized tournaments")
  case ModMessage extends Permission("MOD_MESSAGE", "Send mod messages")
  case Impersonate extends Permission("IMPERSONATE", "Impersonate")
  case DisapproveCoachReview extends Permission("DISAPPROVE_COACH_REVIEW", "Disapprove coach review")
  case PayPal extends Permission("PAYPAL", "PayPal")
  // Set the tier of own broadcasts, making them official. Group own broadcasts.
  case Relay extends Permission("RELAY", "Broadcast official")
  case Cli extends Permission("CLI", "Command line")
  case Settings extends Permission("SETTINGS", "Lila settings")
  case Streamers extends Permission("STREAMERS", "Manage streamers")
  case Verified extends Permission("VERIFIED", "Verified badge")
  case Pages extends Permission("PAGES", "Lichess pages")
  case Feed extends Permission("DAILY_FEED", "Feed updates")
  case MonitoredCheatMod extends Permission("MONITORED_MOD_CHEAT", "Monitored mod: cheat")
  case MonitoredBoostMod extends Permission("MONITORED_MOD_BOOST", "Monitored mod: boost")
  case MonitoredCommMod extends Permission("MONITORED_MOD_COMM", "Monitored mod: comms")
  case StudyAdmin extends Permission("STUDY_ADMIN", List(Relay), "Study/Broadcast admin")
  case ApiHog extends Permission("API_HOG", "API hog")
  case ApiChallengeAdmin extends Permission("API_CHALLENGE_ADMIN", "API Challenge admin")
  case LichessTeam extends Permission("LICHESS_TEAM", List(Beta), "Lichess team")
  case BotEditor extends Permission("BOT_EDITOR", "Bot editor")
  case TimeoutMod
      extends Permission(
        "TIMEOUT_MOD",
        List(LichessTeam, ChatTimeout, PublicChatView, GamifyView),
        "Timeout mod"
      )
  case BoostHunter
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
          SeeInsight,
          ModMessage,
          ModNote,
          ViewPrintNoIP,
          SendToZulip
        ),
        "Boost Hunter"
      )
  case CheatHunter
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
          ModMessage,
          ModNote,
          ViewPrintNoIP,
          SendToZulip
        ),
        "Cheat Hunter"
      )
  case Shusher
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
  case EmailAnswerer
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
  case Admin
      extends Permission(
        "ADMIN",
        List(
          BotEditor,
          LichessTeam,
          UserSearch,
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
          FreePatron,
          ManageTeam,
          ManageTournament,
          ManageSimul,
          ManageEvent,
          PracticeConfig,
          PuzzleCurator,
          OpeningWiki,
          Presets,
          Pages,
          Streamers,
          DisableTwoFactor,
          ChangePermission,
          StudyAdmin,
          BroadcastTimeout,
          ApiChallengeAdmin,
          Feed
        ),
        "Admin"
      )
  case SuperAdmin
      extends Permission(
        "SUPER_ADMIN",
        List(
          Admin,
          GdprErase,
          Impersonate,
          FullCommsExport,
          PayPal,
          Cli,
          Settings,
          TitleRequest
        ),
        "Super Admin"
      )

object Permission:

  type Selector = Permission.type => Permission

  val all: Set[Permission] = values.toSet

  val nonModPermissions: Set[Permission] =
    Set(Beta, LichessTeam, Coach, Teacher, Developer, Verified, ContentTeam, BroadcastTeam, ApiHog, BotEditor)

  val modPermissions: Set[Permission] = all.diff(nonModPermissions)

  val allByDbKey: Map[RoleDbKey, Permission] = all.mapBy(_.dbKey)

  def apply(u: User): Set[Permission] = ofDbKeys(u.roles)
  def ofDbKey(dbKey: RoleDbKey): Option[Permission] = allByDbKey.get(dbKey)
  def ofDbKeys(dbKeys: Seq[RoleDbKey]): Set[Permission] = dbKeys.flatMap(allByDbKey.get).toSet
