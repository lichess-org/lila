package lila.security

import lila.core.perm.Permission

object Granter:

  export lila.core.perm.Granter.*

  def canViewAltUsername(user: User)(using Option[Me]): Boolean =
    opt(_.Admin) || {
      (opt(_.CheatHunter) && user.marks.engine) ||
      (opt(_.BoostHunter) && user.marks.boost) ||
      (opt(_.Shusher) && user.marks.troll)
    }

  def canCloseAlt(using Me) = apply(_.CloseAccount) && apply(_.ViewPrintNoIP)

  def canGrant(permission: Permission)(using me: Me): Boolean =
    apply(_.SuperAdmin) || {
      apply(_.ChangePermission) && Permission.nonModPermissions(permission)
    } || {
      apply(_.Admin) && {
        apply(permission) || Set[Permission](
          Permission.MonitoredCheatMod,
          Permission.MonitoredBoostMod,
          Permission.MonitoredCommMod,
          Permission.PublicMod
        )(permission)
      }
    }
