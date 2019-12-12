package lila.security

import lila.user.User

object Granter {

  def apply(permission: Permission)(user: User): Boolean =
    Permission(user.roles) exists (_ is permission)

  def apply(f: Permission.Selector)(user: User): Boolean =
    apply(f(Permission))(user)

  def canGrant(user: User, permission: Permission): Boolean = apply(_.SuperAdmin)(user) || {
    apply(_.ChangePermission)(user) &&
      Set[Permission](
        Permission.Coach,
        Permission.Developer,
        Permission.PublicMod,
        Permission.Verified,
        Permission.Prismic
      ).contains(permission)
  }
}
