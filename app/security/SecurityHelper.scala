package lila
package security

import user.User
import http.Context

trait SecurityHelper {

  def isGranted(permission: Permission)(implicit ctx: Context): Boolean =
    ctx.me.fold(Granter(permission), false)

  def isGranted(permission: Permission, user: User): Boolean =
    Granter(permission)(user)
}
