package lila.security

import lila.user.User

object Granter {

  def apply(permission: Permission)(user: User): Boolean =
    Permission(user.roles) exists (_ is permission)

  def option(permission: Permission)(user: Option[User]): Boolean =
    ~user.map(apply(permission))

  def superAdmin(user: User): Boolean =  apply(Permission.SuperAdmin)(user)

  def superAdmin(user: Option[User]): Boolean = ~(user map superAdmin)
}
