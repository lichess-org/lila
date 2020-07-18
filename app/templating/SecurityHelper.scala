package lila.app
package templating

import lila.app.ui.ScalatagsTemplate._
import lila.security.{ Granter, Permission }
import lila.user.{ User, UserContext }

trait SecurityHelper {

  def isGranted(permission: Permission.Selector)(implicit ctx: UserContext): Boolean =
    isGranted(permission(Permission))

  def isGranted(permission: Permission)(implicit ctx: UserContext): Boolean =
    ctx.me ?? Granter(permission)

  def isGranted(permission: Permission.Selector, user: User): Boolean =
    isGranted(permission(Permission), user)

  def isGranted(permission: Permission, user: User): Boolean =
    Granter(permission)(user)

  def canGrant = Granter.canGrant _

  def canViewRoles(user: User)(implicit ctx: UserContext): Boolean =
    isGranted(_.ChangePermission) || (isGranted(_.Admin) && user.roles.nonEmpty)

  def reportScore(score: lila.report.Report.Score): Frag =
    span(cls := s"score ${score.color}")(score.value.toInt)
}
