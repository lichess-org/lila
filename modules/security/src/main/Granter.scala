package lila.security

import lila.user.{ Holder, User }

object Granter:

  def apply(permission: Permission)(user: User): Boolean =
    user.enabled.yes && apply(permission, user.roles)

  def apply(f: Permission.Selector)(user: User): Boolean =
    user.enabled.yes && apply(f(Permission), user.roles)

  def is(permission: Permission)(holder: Holder): Boolean =
    apply(permission)(holder.user)

  def is(f: Permission.Selector)(holder: Holder): Boolean =
    apply(f)(holder.user)

  def apply(permission: Permission, roles: Seq[String]): Boolean =
    Permission(roles).exists(_ is permission)

  def byRoles(f: Permission.Selector)(roles: Seq[String]): Boolean =
    apply(f(Permission), roles)

  def canGrant(user: Holder, permission: Permission): Boolean =
    is(_.SuperAdmin)(user) || {
      is(_.ChangePermission)(user) && Permission.nonModPermissions(permission)
    } || {
      is(_.Admin)(user) && {
        is(permission)(user) || Set[Permission](
          Permission.MonitoredMod,
          Permission.PublicMod
        )(permission)
      }
    }

  def canViewAltUsername(mod: Holder, user: User): Boolean =
    is(_.Admin)(mod) || {
      (is(_.CheatHunter)(mod) && user.marks.engine) ||
      (is(_.BoostHunter)(mod) && user.marks.boost) ||
      (is(_.Shusher)(mod) && user.marks.troll)
    }

  def canCloseAlt(mod: Holder) = apply(_.CloseAccount)(mod.user) && apply(_.ViewPrintNoIP)(mod.user)
