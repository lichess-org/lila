package lila.app
package templating

import lila.app.ui.ScalatagsTemplate.*
import lila.core.perm.{ Granter, Permission }
import lila.core.user.User

trait SecurityHelper:

  def isGranted(f: Permission.Selector)(using Option[Me]): Boolean =
    Granter.opt[Me](f)

  def isGranted(permission: Permission.Selector, user: User): Boolean =
    Granter.ofUser(permission)(user)

  def isGranted(permission: Permission, user: User): Boolean =
    Granter.ofUser(_ => permission)(user)

  def reportScore(score: lila.report.Report.Score): Frag =
    span(cls := s"score ${score.color}")(score.value.toInt)

  def canCloseAlt(using me: Option[Me]): Boolean = me.soUse(lila.security.Granter.canCloseAlt)
