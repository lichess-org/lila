package lila.security

import lila.user.User
import lila.http.Context

trait SecurityHelper {

  def isGranted(permission: Permission)(implicit ctx: Context): Boolean =
    ~ctx.me.map(Granter(permission))

  def isGranted(permission: Permission, user: User): Boolean =
    Granter(permission)(user)
}
