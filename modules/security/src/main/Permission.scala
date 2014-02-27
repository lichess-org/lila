package lila.security

sealed abstract class Permission(val name: String, val children: List[Permission] = Nil) {

  final def is(p: Permission): Boolean =
    this == p || (children exists (_ is p))
}

object Permission {

  case object ViewBlurs extends Permission("ROLE_VIEW_BLURS")
  case object StaffForum extends Permission("ROLE_STAFF_FORUM")
  case object ModerateForum extends Permission("ROLE_MODERATE_FORUM")

  case object UserSpy extends Permission("ROLE_USER_SPY")
  case object UserEvaluate extends Permission("ROLE_USER_EVALUATE")
  case object MarkTroll extends Permission("ROLE_CHAT_BAN", List(UserSpy))
  case object MarkEngine extends Permission("ROLE_ADJUST_CHEATER", List(UserSpy))
  case object IpBan extends Permission("ROLE_IP_BAN", List(UserSpy))
  case object CloseAccount extends Permission("ROLE_CLOSE_ACCOUNT", List(UserSpy))
  case object ReopenAccount extends Permission("ROLE_REOPEN_ACCOUNT", List(UserSpy))
  case object SetTitle extends Permission("ROLE_SET_TITLE", List(UserSpy))
  case object SeeReport extends Permission("ROLE_SEE_REPORT", Nil)

  case object Hunter extends Permission("ROLE_HUNTER", List(
    ViewBlurs, MarkEngine, StaffForum, UserSpy, UserEvaluate, SeeReport))

  case object Admin extends Permission("ROLE_ADMIN", List(
    ViewBlurs, MarkTroll, MarkEngine, StaffForum, ModerateForum, UserSpy,
    UserEvaluate, SeeReport, IpBan, CloseAccount, ReopenAccount, SetTitle))

  case object SuperAdmin extends Permission("ROLE_SUPER_ADMIN", List(Admin))

  private lazy val all: List[Permission] = List(SuperAdmin, Admin, Hunter, ViewBlurs, StaffForum, ModerateForum, UserSpy, MarkTroll, MarkEngine, IpBan)
  private lazy val allByName: Map[String, Permission] = all map { p => (p.name, p) } toMap

  def apply(name: String): Option[Permission] = allByName get name

  def apply(names: List[String]): List[Permission] = (names map apply).flatten

  def exists(name: String) = allByName contains name
}
