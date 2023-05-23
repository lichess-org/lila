package lila.app
package templating

import lila.app.ui.ScalatagsTemplate.*
import lila.security.{ Granter, Permission }
import lila.user.{ User, UserContext, Holder }

trait SecurityHelper:

  def isGranted(permission: Permission.Selector)(using UserContext): Boolean =
    isGranted(permission(Permission))

  def isGranted(permission: Permission)(using ctx: UserContext): Boolean =
    ctx.me ?? Granter(permission)

  def isGranted(permission: Permission.Selector, user: User): Boolean =
    isGranted(permission(Permission), user)

  def isGranted(permission: Permission, user: User): Boolean =
    Granter(permission)(user)

  def canGrant = Granter.canGrant

  def canViewRoles(user: User)(using UserContext): Boolean =
    isGranted(_.ChangePermission) || (isGranted(_.Admin) && user.roles.nonEmpty)

  def reportScore(score: lila.report.Report.Score): Frag =
    span(cls := s"score ${score.color}")(score.value.toInt)

  def canCloseAlt(using ctx: UserContext): Boolean = ctx.me.map(Holder.apply) ?? Granter.canCloseAlt
