package lila.security

import lila.user.User

object Granter {

  def apply(permission: Permission)(user: User): Boolean =
    Permission(user.roles) exists (_ is permission)

  def apply(f: Permission.Selector)(user: User): Boolean =
    apply(f(Permission))(user)

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
