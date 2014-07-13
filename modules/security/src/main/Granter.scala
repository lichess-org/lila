package lila.security

import lila.user.User

object Granter {

  def apply(permission: Permission)(user: User): Boolean =
    Permission(user.roles) exists (_ is permission)

  def apply(f: Permission.type => Permission)(user: User): Boolean =
    apply(f(Permission))(user)

  def superAdmin(user: User): Boolean = apply(Permission.SuperAdmin)(user)
}
