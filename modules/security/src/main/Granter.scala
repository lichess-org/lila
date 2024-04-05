package lila.security

import lila.core.security.Grantable
import lila.core.user.User

object Granter:

  def apply[U](permission: Permission)(using me: Grantable): Boolean =
    me.enabled.yes && apply(permission, me.roles)

  def apply(f: Permission.Selector)(using me: Grantable): Boolean =
    me.enabled.yes && apply(f(Permission), me.roles)

  def opt(f: Permission.Selector)(using me: Option[Grantable]): Boolean =
    me.fold(false)(apply(f)(using _))

  def of(permission: Permission)(user: User): Boolean =
    user.enabled.yes && apply(permission, user.roles)

  def of(f: Permission.Selector)(user: User): Boolean =
    user.enabled.yes && apply(f(Permission), user.roles)

  def apply(permission: Permission, roles: Seq[String]): Boolean =
    Permission(roles).exists(_.is(permission))

  def byRoles(f: Permission.Selector)(roles: Seq[String]): Boolean =
    apply(f(Permission), roles)

  def canGrant(permission: Permission)(using Grantable): Boolean =
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

  def canViewAltUsername(user: User)(using Option[Grantable]): Boolean =
    opt(_.Admin) || {
      (opt(_.CheatHunter) && user.marks.engine) ||
      (opt(_.BoostHunter) && user.marks.boost) ||
      (opt(_.Shusher) && user.marks.troll)
    }

  def canCloseAlt(using Grantable) = apply(_.CloseAccount) && apply(_.ViewPrintNoIP)
