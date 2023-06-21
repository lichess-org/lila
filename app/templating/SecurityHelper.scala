package lila.app
package templating

import lila.app.ui.ScalatagsTemplate.*

import lila.security.{ Granter, Permission }
import lila.user.{ User, Me }

trait SecurityHelper:

  export Granter.canGrant

  def isGranted(permission: Permission.Selector)(using ctx: Context): Boolean =
    ctx.me soUse Granter.opt(permission)

  def isGranted(permission: Permission.Selector, user: User): Boolean =
    isGranted(permission(Permission), user)

  def isGranted(permission: Permission, user: User): Boolean =
    Granter.of(permission)(user)

  def canViewRoles(user: User)(using Option[Me]): Boolean =
    Granter.opt(_.ChangePermission) || (Granter.opt(_.Admin) && user.roles.nonEmpty)

  def reportScore(score: lila.report.Report.Score): Frag =
    span(cls := s"score ${score.color}")(score.value.toInt)

  def canCloseAlt(using me: Option[Me]): Boolean = me soUse Granter.canCloseAlt
