package lila.app
package templating

import lila.user.{ User, Context }
import lila.security.{ Permission, Granter }

trait SecurityHelper {

  def isGranted(permission: Permission)(implicit ctx: Context): Boolean =
    ctx.me zmap Granter(permission)

  def isGranted(permission: Permission, user: User): Boolean =
    Granter(permission)(user)
}
