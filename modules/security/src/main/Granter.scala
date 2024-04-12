package lila.security

import lila.core.perm.Grantable
import lila.core.perm.Permission

object Granter:

  export lila.core.perm.Granter.*

  def canViewAltUsername(user: User)(using Option[Me]): Boolean =
    opt[Me](_.Admin) || {
      (opt[Me](_.CheatHunter) && user.marks.engine) ||
      (opt[Me](_.BoostHunter) && user.marks.boost) ||
      (opt[Me](_.Shusher) && user.marks.troll)
    }

  def canCloseAlt(using Me) = apply[Me](_.CloseAccount) && apply[Me](_.ViewPrintNoIP)

  def canGrant[U: Grantable](permission: Permission)(using me: U): Boolean =
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
