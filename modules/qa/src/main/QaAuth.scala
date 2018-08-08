package lidraughts.qa

import lidraughts.security.Granter
import lidraughts.user.{ User, UserContext }
import org.joda.time.DateTime

object QaAuth {

  def canEdit(q: Question)(implicit ctx: UserContext) = noTroll { u =>
    (q ownBy u) || Granter(_.ModerateQa)(u)
  }

  def canEdit(a: Answer)(implicit ctx: UserContext) = noTroll { u =>
    (a ownBy u) || Granter(_.ModerateQa)(u)
  }

  def canAsk(implicit ctx: UserContext) = noKid(noTroll(isNotN00b))

  def canAnswer(q: Question)(implicit ctx: UserContext) = noLock(q)(noKid(noTroll(isNotN00b)))

  def canVote(implicit ctx: UserContext) = noKid(noTroll(isNotN00b))

  def canComment(implicit ctx: UserContext) = noKid(noTroll(isNotN00b))

  private def noKid(res: => Boolean)(implicit ctx: UserContext) = ctx.noKid && res

  private def noTroll(block: User => Boolean)(implicit ctx: UserContext) =
    ctx.me.filterNot(_.troll) ?? block

  private def isNotN00b(u: User) = !isN00b(u)

  def isN00b(u: User) = u.createdAt isAfter DateTime.now.minusWeeks(1)

  def noLock(q: Question)(res: => Boolean)(implicit ctx: UserContext) =
    (!q.isLocked || ctx.me.??(Granter(_.ModerateQa))) && res
}
