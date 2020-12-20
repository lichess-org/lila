package lila.security

import lila.user.User

object Granter {

  def apply(permission: Permission)(user: User): Boolean =
    apply(permission, user.roles)

  def apply(f: Permission.Selector)(user: User): Boolean =
    apply(f(Permission), user.roles)

  def apply(permission: Permission, roles: Seq[String]): Boolean =
    Permission(roles).exists(_ is permission)

  def byRoles(f: Permission.Selector)(roles: Seq[String]): Boolean =
    apply(f(Permission), roles)

  def canGrant(user: User, permission: Permission): Boolean =
    apply(_.SuperAdmin)(user) || {
      apply(_.ChangePermission)(user) && Permission.nonModPermissions(permission)
    } || {
      apply(_.Admin)(user) && {
        apply(permission)(user) || Set[Permission](
          Permission.MonitoredMod,
          Permission.PublicMod
        )(permission)
      }
    }
}
