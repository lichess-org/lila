package lila.app
package templating

import lila.user.{ User, UserContext }
import lila.security.{ Permission, Granter }

trait SecurityHelper {

  def isGranted(permission: Permission.type => Permission)(implicit ctx: UserContext): Boolean =
    isGranted(permission(Permission))

  def isGranted(permission: Permission)(implicit ctx: UserContext): Boolean =
    ctx.me ?? Granter(permission)

  def isGranted(permission: Permission.type => Permission, user: User): Boolean =
    isGranted(permission(Permission), user)

  def isGranted(permission: Permission, user: User): Boolean =
    Granter(permission)(user)
}
