package lila.security

import lila.user.{ Holder, User, Me }

object Granter:

  def apply(permission: Permission)(using me: Me): Boolean =
    me.enabled.yes && apply(permission, me.roles)

  def apply(f: Permission.Selector)(using me: Me): Boolean =
    me.enabled.yes && apply(f(Permission), me.roles)

  def opt(f: Permission.Selector)(using me: Option[Me]): Boolean =
    me.fold(false)(apply(f)(using _))

  def is(permission: Permission)(holder: Holder): Boolean =
    of(permission)(holder.user)

  def is(f: Permission.Selector)(holder: Holder): Boolean =
    of(f)(holder.user)

  def of(permission: Permission)(user: User): Boolean =
    user.enabled.yes && apply(permission, user.roles)

  def of(f: Permission.Selector)(user: User): Boolean =
    user.enabled.yes && apply(f(Permission), user.roles)

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

  def canCloseAlt(using me: Me) = apply(_.CloseAccount) && apply(_.ViewPrintNoIP)
