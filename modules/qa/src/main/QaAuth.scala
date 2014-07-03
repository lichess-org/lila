package lila.qa

import lila.user.UserContext
import lila.security.Granter

object QaAuth {

  def canEdit(q: Question)(implicit ctx: UserContext) = ctx.me ?? { u =>
    (q ownBy u) || Granter(_.ModerateQa)(u)
  }

  def canEdit(a: Answer)(implicit ctx: UserContext) = ctx.me ?? { u =>
    (a ownBy u) || Granter(_.ModerateQa)(u)
  }
}
