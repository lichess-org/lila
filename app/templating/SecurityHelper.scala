package lidraughts.app
package templating

import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.security.{ Permission, Granter }
import lidraughts.user.{ User, UserContext }

trait SecurityHelper {

  def isGranted(permission: Permission.Selector)(implicit ctx: UserContext): Boolean =
    isGranted(permission(Permission))

  def isGranted(permission: Permission)(implicit ctx: UserContext): Boolean =
    ctx.me ?? Granter(permission)

  def isGranted(permission: Permission.Selector, user: User): Boolean =
    isGranted(permission(Permission), user)

  def isGranted(permission: Permission, user: User): Boolean =
    Granter(permission)(user)

  def canViewRoles(user: User)(implicit ctx: UserContext): Boolean =
    isGranted(_.ChangePermission) || (isGranted(_.Admin) && user.roles.nonEmpty)

  def reportScore(score: lidraughts.report.Report.Score): Frag = raw {
    s"""<div class="score ${score.color}" title="Report score">${score.value.toInt}</div>"""
  }
}
