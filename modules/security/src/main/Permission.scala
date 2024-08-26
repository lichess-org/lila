package lila.security

import lila.core.perm.Permission
import lila.core.user.RoleDbKey

object Permission:

  import lila.core.perm.Permission.*

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
      TitleRequest,
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
      Pages,
      Relay,
      BroadcastTimeout,
      ManageEvent,
      ManageTournament,
      ManageSimul,
      StudyAdmin,
      PracticeConfig,
      PuzzleCurator,
      OpeningWiki,
      Presets,
      Feed
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
      Coach,
      Teacher,
      ApiHog,
      ApiChallengeAdmin
    ),
    "Badge" -> List(
      Developer,
      PublicMod,
      Verified,
      ContentTeam,
      BroadcastTeam
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

  def expanded(u: User): Set[Permission] =
    val level0 = apply(u)
    val level1 = level0.flatMap(_.alsoGrants)
    val level2 = level1.flatMap(_.alsoGrants)
    level0 ++ level1 ++ level2

  val form =
    import play.api.data.Form
    import play.api.data.Forms.*
    import lila.common.Form.*
    Form(single("permissions" -> list[RoleDbKey](text.into[RoleDbKey].verifying(allByDbKey.contains))))
