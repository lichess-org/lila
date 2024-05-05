package lila.web
package ui

import lila.core.perm.{ Granter, Permission }
import lila.core.user.User

trait SecurityHelper:

  def isGranted(f: Permission.Selector)(using Option[Me]): Boolean =
    Granter.opt(f)

  def isGranted(permission: Permission.Selector, user: User): Boolean =
    Granter.ofUser(permission)(user)

  def isGranted(permission: Permission, user: User): Boolean =
    Granter.ofUser(_ => permission)(user)
