package lila.security

import lila.user.{ User, Context }

trait SecurityHelper {

  def isGranted(permission: Permission)(implicit ctx: Context): Boolean =
    ctx.me ?? Granter(permission)

  def isGranted(permission: Permission, user: User): Boolean =
    Granter(permission)(user)
}
