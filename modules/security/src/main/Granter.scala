package lila.security

import lila.core.perm.Permission

object Granter:

  export lila.core.perm.Granter.*

  def canViewAltUsername(user: User)(using Option[Me]): Boolean =
    opt(_.Admin) || {
      (opt(_.CheatHunter) && user.marks.engine) ||
      (opt(_.BoostHunter) && user.marks.boost) ||
      (opt(_.Shusher) && user.marks.troll)
    }

  def canFullyLogin(u: User) = u.enabled.yes || !(u.lameOrTroll || u.marks.alt)
