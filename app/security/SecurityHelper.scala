package lila
package security

import http.Context

trait SecurityHelper {

  def isGranted(permission: Permission)(implicit ctx: Context): Boolean =
    ctx.me.fold(_ hasRole permission.name, false)
}
